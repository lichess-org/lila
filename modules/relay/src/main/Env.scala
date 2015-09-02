package lila.relay

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import makeTimeout.short

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    lightUser: String => Option[lila.common.LightUser],
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

  private val settings = new {
    val Enabled = config getBoolean "enabled"
    val UserId = config getString "user_id"
    val ImportMoveDelay = config duration "import.move_delay"
    val CollectionRelay = config getString "collection.relay"
    val CollectionContent = config getString "collection.content"
    val TourneyActorMapName = config getString "actor.map.tourney.name"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
  }
  import settings._

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

  private val relayColl = db(CollectionRelay)

  val repo = new RelayRepo(relayColl)

  lazy val jsonView = new JsonView

  lazy val contentApi = new ContentApi(db(CollectionContent))

  lazy val api = new RelayApi(
    relayColl,
    contentApi,
    mainFics,
    repo,
    tourneyMap)

  private val importer = new Importer(
    hub.actor.roundMap,
    ImportMoveDelay,
    system.scheduler)

  private val tourneyMap = system.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(id: String) = new TourneyActor(
      id = id,
      ficsProps = ficsProps,
      repo = repo,
      importer = importer)
    def receive = actorMapReceive
  }), name = TourneyActorMapName)

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(relayId: String) = new Socket(
        relayId = relayId,
        history = new lila.socket.History(ttl = HistoryMessageTtl),
        getRelay = () => repo byId relayId,
        jsonView = jsonView,
        lightUser = lightUser,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout)
    }), name = SocketName)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood,
    exists = repo.exists)

  lazy val cached = new Cached(repo)

  def version(relayId: String): Fu[Int] =
    socketHub ? lila.hub.actorApi.map.Ask(
      relayId,
      lila.socket.actorApi.GetVersion) mapTo manifest[Int]

  {
    import scala.concurrent.duration._

    scheduler.effect(2 minutes, "refresh FICS relays") {
      api.refreshFromFics
    }
    scheduler.effect(1 minutes, "sort FICS relays") {
      api.setElo
    }
  }
}

object Env {

  lazy val current = "relay" boot new Env(
    config = lila.common.PlayApp loadConfig "relay",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    scheduler = lila.common.PlayApp.scheduler)
}
