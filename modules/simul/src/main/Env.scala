package lidraughts.simul

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.hub.actorApi.map.Ask
import lidraughts.hub.{ ActorMap, Sequencer }
import lidraughts.socket.actorApi.GetVersion
import lidraughts.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    lightUser: lidraughts.common.LightUser.Getter,
    onGameStart: String => Unit,
    isOnline: String => Boolean,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionSimul = config getString "collection.simul"
    val SequencerTimeout = config duration "sequencer.timeout"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val repo = new SimulRepo(
    simulColl = simulColl
  )

  lazy val api = new SimulApi(
    repo = repo,
    system = system,
    socketHub = socketHub,
    site = hub.socket.site,
    renderer = hub.actor.renderer,
    timeline = hub.actor.timeline,
    userRegister = hub.actor.userRegister,
    lobby = hub.socket.lobby,
    onGameStart = onGameStart,
    sequencers = sequencerMap,
    asyncCache = asyncCache
  )

  lazy val forms = new DataForm

  lazy val jsonView = new JsonView(lightUser)

  private val socketHub = system.actorOf(
    Props(new lidraughts.socket.SocketHubActor.Default[Socket] {
      def mkActor(simulId: String) = new Socket(
        simulId = simulId,
        history = new History(ttl = HistoryMessageTtl),
        getSimul = repo.find,
        jsonView = jsonView,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightUser = lightUser
      )
    }), name = SocketName
  )

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    exists = repo.exists
  )

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    import akka.pattern.pipe
    def receive = {
      case lidraughts.game.actorApi.FinishGame(game, _, _) => api finishGame game
      case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) => api ejectCheater userId
      case lidraughts.hub.actorApi.simul.GetHostIds => api.currentHostIds pipeTo sender
      case lidraughts.hub.actorApi.round.SimulMoveEvent(move, simulId, opponentUserId) =>
        hub.actor.userRegister ! lidraughts.hub.actorApi.SendTo(
          opponentUserId,
          lidraughts.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
        )
    }
  }), name = ActorName), 'finishGame, 'adjustCheater, 'moveEventSimul)

  def isHosting(userId: String): Fu[Boolean] = api.currentHostIds map (_ contains userId)

  val allCreated = asyncCache.single(
    name = "simul.allCreated",
    repo.allCreated,
    expireAfter = _.ExpireAfterWrite(CreatedCacheTtl)
  )

  val allCreatedFeaturable = asyncCache.single(
    name = "simul.allCreatedFeaturable",
    repo.allCreatedFeaturable,
    expireAfter = _.ExpireAfterWrite(CreatedCacheTtl)
  )

  def version(tourId: String): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  private[simul] val simulColl = db(CollectionSimul)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(SequencerTimeout.some, logger = logger)
  }))

  private lazy val simulCleaner = new SimulCleaner(repo, api, socketHub)

  scheduler.effect(15 seconds, "[simul] cleaner")(simulCleaner.apply)
}

object Env {

  lazy val current = "simul" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "simul",
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler,
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    lightUser = lidraughts.user.Env.current.lightUser,
    onGameStart = lidraughts.game.Env.current.onStart,
    isOnline = lidraughts.user.Env.current.isOnline,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
