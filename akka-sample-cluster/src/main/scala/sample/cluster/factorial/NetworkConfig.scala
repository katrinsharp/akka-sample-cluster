package sample.cluster.factorial

import java.net.NetworkInterface
import java.net.InetAddress

import scala.collection.JavaConversions._
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
  * Created by Admin on 2016-08-31.
  */
object NetworkConfig {

  def hostLocalAddress: String = NetworkInterface.getNetworkInterfaces.
        find(_.getName equals "eth0").
        flatMap(interface =>
          interface.getInetAddresses.find(_.isSiteLocalAddress).map(_.getHostAddress)).
        getOrElse("127.0.0.1")

  def seedNodesIps: Seq[String] = Option(System.getenv("SEED_DISCOVERY_SERVICE")).
      map(InetAddress.getAllByName(_).map(_.getHostAddress).toSeq).
      getOrElse(Seq.empty)

  def seedNodesPorts: Seq[String] = Option(System.getenv("SEED_PORT")).
    map(port => Seq.fill(seedNodesIps.size)(port)).getOrElse(Seq.empty)

  def seedsConfig(config: Config, clusterName: String): Config =
    if(!seedNodesIps.isEmpty)
      ConfigFactory.empty().withValue("akka.cluster.seed-nodes",
        ConfigValueFactory.fromIterable(seedNodesIps.zip(seedNodesPorts).
          map{case (ip, port) => s"akka.tcp://$clusterName@$ip:$port"}))
    else ConfigFactory.empty()
}
