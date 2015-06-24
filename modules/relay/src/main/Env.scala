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

  /* ACTOR ARCHITECTURE
   *
   * system                   ActorSystem
   * +- 1 fics                ActorFSM
   * |  +- 1 telnet           Actor
   * +- 1 tourneyMap          ActorMap
   *    +- x tourney          SequentialActor
   *    |  +- 1 fics          ActorFSM
   *    |     +- 1 telnet     Actor
   *    +- 1 gameMap          ActorMap
   *       +- x game          SequentialActor
   *          +- 1 fics       ActorFSM // reuse tourney fics ref
   *             +- 1 telnet  Actor
   */

  private val Enabled = config getBoolean "enabled"
  private val UserId = config getString "user_id"
  private val ImportMoveDelay = config duration "import.move_delay"
  private val CollectionRelay = config getString "collection.relay"
  private val TourneyActorMapName = config getString "actor.map.tourney.name"

  private val ficsConfig = FICS.Config(
    host = config getString "fics.host",
    port = config getInt "fics.port",
    login = config getString "fics.login",
    password = config getString "fics.password",
    enabled = Enabled)

  private val ficsProps =
    if (ficsConfig.enabled) Props(classOf[FICS], ficsConfig)
    else Props(classOf[FICStub])

  private val mainFics = system.actorOf(ficsProps, name = "fics")

  private lazy val relayRepo = new RelayRepo(db(CollectionRelay))

  lazy val api = new RelayApi(mainFics, relayRepo, tourneyMap)

  private val importer = new Importer(
    roundMap,
    ImportMoveDelay,
    system.scheduler)

  private val tourneyMap = system.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(id: String) = new TourneyActor(
      id = id,
      ficsProps = ficsProps,
      repo = relayRepo,
      importer = importer)
    def receive = actorMapReceive
  }), name = TourneyActorMapName)

  {
    import scala.concurrent.duration._

    api.refreshFromFics
    // scheduler.effect(1 minutes, "refresh FICS relays") {
    //   api.refreshFromFics
    // }
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
