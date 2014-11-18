package lila.relay

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    liveImporter: lila.importer.Live,
    system: ActorSystem) {

  private val UserId = config getString "user_id"

  private val relayFSM = system.actorOf(Props(
    classOf[RelayFSM],
    liveImporter,
    UserId
  ), name = "fsm")

  private val telnetActor = system.actorOf(Props(
    classOf[Telnet],
    new java.net.InetSocketAddress("freechess.org", 5000),
    relayFSM
  ), name = "relay.telnet")
}

object Env {

  lazy val current = "[boot] relay" describes new Env(
    config = lila.common.PlayApp loadConfig "relay",
    liveImporter = lila.importer.Env.current.live,
    system = lila.common.PlayApp.system)
}
