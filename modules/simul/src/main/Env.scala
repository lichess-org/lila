package lila.simul

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import lila.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMap: ActorRef,
    lightUser: String => Option[lila.common.LightUser],
    onGameStart: String => Unit,
    isOnline: String => Boolean) {

  private val settings = new {
    val CollectionSimul = config getString "collection.simul"
    val SequencerTimeout = config duration "sequencer.timeout"
    val SequencerMapName = config getString "sequencer.map_name"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
  }
  import settings._

  lazy val repo = new SimulRepo(
    simulColl = simulColl)

  lazy val api = new SimulApi(
    repo = repo,
    socketHub = socketHub,
    onGameStart = onGameStart,
    sequencers = sequencerMap)

  lazy val forms = new DataForm

  lazy val jsonView = new JsonView(lightUser)

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(simulId: String) = new Socket(
        simulId = simulId,
        history = new History(ttl = HistoryMessageTtl),
        getSimul = repo.find,
        jsonView = jsonView,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightUser = lightUser)
    }), name = SocketName)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood,
    exists = repo.exists)

  system.actorOf(Props(new Actor {
    override def preStart() {
      system.lilaBus.subscribe(self, 'finishGame, 'adjustCheater)
    }
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => api finishGame game
      case lila.hub.actorApi.mod.MarkCheater(userId) => api ejectCheater userId
    }
  }))

  val allCreated = lila.memo.AsyncCache.single(repo.allCreated, timeToLive = CreatedCacheTtl)

  def version(tourId: String): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  private[simul] val simulColl = db(CollectionSimul)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(SequencerTimeout)
  }), name = SequencerMapName)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] simul" describes new Env(
    config = lila.common.PlayApp loadConfig "simul",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMap = lila.round.Env.current.roundMap,
    lightUser = lila.user.Env.current.lightUser,
    onGameStart = lila.game.Env.current.onStart,
    isOnline = lila.user.Env.current.isOnline)
}
