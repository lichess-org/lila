package lila.relay

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem,
    roundMap: akka.actor.ActorRef) {

  private val UserId = config getString "user_id"
  private val ImportIP = config getString "import.ip"
  private val ImportMoveDelay = config duration "import.move_delay"
  private val FicsHost = config getString "fics.host"
  private val FicsPort = config getInt "fics.port"

  private val importer = new Importer(
    roundMap,
    ImportMoveDelay,
    ImportIP,
    system.scheduler)

  private val relayFSM = system.actorOf(Props(
    classOf[RelayFSM],
    importer
  ), name = "fsm")

  private val telnetActor = system.actorOf(Props(
    classOf[Telnet],
    new java.net.InetSocketAddress(FicsHost, FicsPort),
    relayFSM
  ), name = "relay.telnet")
}

object Env {

  lazy val current = "[boot] relay" describes new Env(
    config = lila.common.PlayApp loadConfig "relay",
    system = lila.common.PlayApp.system,
    roundMap = lila.round.Env.current.roundMap)
}
