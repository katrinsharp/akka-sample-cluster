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

    val upToN = if (args.isEmpty) 10 else args(0).toInt
    val repeat = if (args.isEmpty) true else args(0) == 't'

    val internalIp = NetworkConfig.hostLocalAddress

    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.bind-hostname=$internalIp")).
      withFallback(ConfigFactory.load("factorial"))

    val system = ActorSystem(config.getString("clustering.name"), config)
    system.log.info("Factorials will start when 2 backend members in the cluster.")
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