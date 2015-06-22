package lila.relay

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    roundMap: akka.actor.ActorRef,
    scheduler: lila.common.Scheduler) {

  private val UserId = config getString "user_id"
  private val ImportIP = config getString "import.ip"
  private val ImportMoveDelay = config duration "import.move_delay"
  private val FicsHost = config getString "fics.host"
  private val FicsPort = config getInt "fics.port"
  private val CollectionRelay = config getString "collection.relay"
  private val ActorMapName = config getString "actor.map.name"

  private lazy val relayRepo = new RelayRepo(db(CollectionRelay))

  private val remote = new java.net.InetSocketAddress(FicsHost, FicsPort)

  lazy val api = new RelayApi(system, relayRepo, gameMap, remote)

  private val importer = new Importer(
    roundMap,
    ImportMoveDelay,
    ImportIP,
    system.scheduler)

  private val gameMap = system.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(ficsId: String) = new GameActor(
      ficsId = parseIntOption(ficsId) err s"Invalid relay FICS id $ficsId",
      getGameId = relayRepo.gameIdByFicsId _,
      importer = importer)
    def receive = actorMapReceive
  }), name = ActorMapName)

  // private val relayFSM = system.actorOf(Props(
  //   classOf[RelayFSM],
  //   importer
  // ), name = "fsm")

  // private val telnetActor = system.actorOf(Props(
  //   classOf[Telnet],
  //   new java.net.InetSocketAddress(FicsHost, FicsPort),
  //   relayFSM,
  //   "fics% "
  // ), name = "relay.telnet")

  {
    import scala.concurrent.duration._

    api.refreshRelays >> api.refreshRelayGames
    scheduler.effect(1 minutes, "refresh FICS relays") {
      api.refreshRelays >> api.refreshRelayGames
    }
  }
}

object Env {

  lazy val current = "[boot] relay" describes new Env(
    config = lila.common.PlayApp loadConfig "relay",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    roundMap = lila.round.Env.current.roundMap,
    scheduler = lila.common.PlayApp.scheduler)
}
