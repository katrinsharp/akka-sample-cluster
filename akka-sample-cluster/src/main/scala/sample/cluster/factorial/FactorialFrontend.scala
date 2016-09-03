package sample.cluster.factorial

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.routing.FromConfig
import akka.actor.ReceiveTimeout

//#frontend
class FactorialFrontend(upToN: Int, repeat: Boolean) extends Actor with ActorLogging {

  val backend = context.actorOf(FromConfig.props(),
    name = "factorialBackendRouter")

  override def preStart(): Unit = {
    sendJobs()
    if (repeat) {
      context.setReceiveTimeout(10.seconds)
    }
  }

  def receive = {
    case (n: Int, factorial: BigInt) =>
      if (n == upToN) {
        log.info("{}! = {} sender: {}", n, factorial, sender().path)
        if (repeat) sendJobs()
        else context.stop(self)
      }
    case ReceiveTimeout =>
      log.info("Timeout")
      sendJobs()
  }

  def sendJobs(): Unit = {
    log.info("Starting batch of factorials up to [{}]", upToN)
    1 to upToN foreach { backend ! _ }
  }
}
//#frontend

object FactorialFrontend {
  def main(args: Array[String]): Unit = {

    val (upToN, repeat) = args.size match {
      case 0 => (10, true)
      case 1 => (args(0).toInt, true)
      case 2 => (args(0).toInt, args(1) == "true")
      case x =>
        println("Format: ...FactorialFrontend 'upToN' 'repeat'")
        System.exit(0)
    }

    val internalIp = NetworkConfig.hostLocalAddress

    val appConfig = ConfigFactory.load("factorial")
    val clusterName = appConfig.getString("clustering.name")
    val minMembers = appConfig.getNumber("akka.cluster.min-nr-of-members")

    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.bind-hostname=$internalIp")).
      withFallback(NetworkConfig.seedsConfig(appConfig, clusterName)).
      withFallback(appConfig)

    val system = ActorSystem(clusterName, config)
    system.log.info(s"Factorials will start when $minMembers backend members in the cluster.")
    //#registerOnUp
    Cluster(system) registerOnMemberUp {
      system.actorOf(Props(classOf[FactorialFrontend], upToN, repeat),
        name = "factorialFrontend")
    }
    //#registerOnUp

    //#registerOnRemoved
    Cluster(system).registerOnMemberRemoved {
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(-1))
      // in case ActorSystem shutdown takes longer than 10 seconds,
      // exit the JVM forcefully anyway
      system.scheduler.scheduleOnce(10.seconds)(System.exit(-1))(system.dispatcher)
      // shut down ActorSystem
      system.shutdown()
    }
    //#registerOnRemoved

  }
}
