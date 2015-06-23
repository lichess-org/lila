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

  private val remote = new java.net.InetSocketAddress(FicsHost, FicsPort)

  private val fics = system.actorOf(Props(classOf[FICS], remote))

  private lazy val relayRepo = new RelayRepo(db(CollectionRelay))

  lazy val api = new RelayApi(fics, relayRepo, gameMap, remote)

  private val importer = new Importer(
    roundMap,
    ImportMoveDelay,
    ImportIP,
    system.scheduler)

  private val gameMap = system.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(ficsIdStr: String) = {
      val ficsId = parseIntOption(ficsIdStr) err s"Invalid relay FICS id $ficsIdStr"
      new GameActor(
        fics = fics,
        ficsId = ficsId,
        getGameId = () => relayRepo gameIdByFicsId ficsId,
        importer = importer)
    }
    def receive = actorMapReceive
  }), name = ActorMapName)

  {
    import scala.concurrent.duration._

    api.refreshFromFics
    scheduler.effect(1 minutes, "refresh FICS relays") {
      api.refreshFromFics
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
