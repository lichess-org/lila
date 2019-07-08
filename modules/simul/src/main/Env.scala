package lila.simul

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.{ Duct, DuctMap, TrouperMap }
import lila.socket.History
import lila.socket.Socket.{ GetVersion, SocketVersion }

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: lila.common.LightUser.Getter,
    onGameStart: String => Unit,
    isOnline: String => Boolean,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val CollectionSimul = config getString "collection.simul"
  private val SequencerTimeout = config duration "sequencer.timeout"
  private val CreatedCacheTtl = config duration "created.cache.ttl"
  private val HistoryMessageTtl = config duration "history.message.ttl"
  private val UidTimeout = config duration "uid.timeout"
  private val SocketTimeout = config duration "socket.timeout"
  private val FeatureViews = config getInt "feature.views"

  lazy val repo = new SimulRepo(
    simulColl = simulColl
  )

  lazy val api = new SimulApi(
    repo = repo,
    system = system,
    socketMap = socketMap,
    renderer = hub.renderer,
    timeline = hub.timeline,
    onGameStart = onGameStart,
    sequencers = sequencerMap,
    asyncCache = asyncCache
  )

  lazy val jsonView = new JsonView(lightUser)

  private val socketMap: SocketMap = lila.socket.SocketMap[Socket](
    system = system,
    mkTrouper = (simulId: String) => new Socket(
      system = system,
      simulId = simulId,
      history = new History(ttl = HistoryMessageTtl),
      getSimul = repo.find,
      jsonView = jsonView,
      uidTtl = UidTimeout,
      lightUser = lightUser,
      keepMeAlive = () => socketMap touch simulId
    ),
    accessTimeout = SocketTimeout,
    monitoringName = "simul.socketMap",
    broomFrequency = 3691 millis
  )

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketMap = socketMap,
    chat = hub.chat,
    exists = repo.exists
  )

  system.lilaBus.subscribeFuns(
    'finishGame -> {
      case lila.game.actorApi.FinishGame(game, _, _) => api finishGame game
    },
    'adjustCheater -> {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) => api ejectCheater userId
    },
    'deploy -> {
      case m: lila.hub.actorApi.Deploy => socketMap tellAll m
    },
    'simulGetHosts -> {
      case lila.hub.actorApi.simul.GetHostIds(promise) => promise completeWith api.currentHostIds
    },
    'moveEventSimul -> {
      case lila.hub.actorApi.round.SimulMoveEvent(move, simulId, opponentUserId) =>
        system.lilaBus.publish(
          lila.hub.actorApi.socket.SendTo(
            opponentUserId,
            lila.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
          ),
          'socketUsers
        )
    }
  )

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

  def featurable(simul: Simul): Boolean = featureLimiter(simul.hostId)(true)

  private val featureLimiter = new lila.memo.RateLimit[lila.user.User.ID](
    credits = FeatureViews,
    duration = 24 hours,
    name = "simul homepage views",
    key = "simul.feature",
    log = false
  )

  def version(simulId: String): Fu[SocketVersion] =
    socketMap.askIfPresentOrZero[SocketVersion](simulId)(GetVersion)

  private[simul] val simulColl = db(CollectionSimul)

  private val sequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu,
    accessTimeout = SequencerTimeout
  )

  private lazy val simulCleaner = new SimulCleaner(repo, api, socketMap)

  scheduler.effect(15 seconds, "[simul] cleaner")(simulCleaner.apply)
}

object Env {

  lazy val current = "simul" boot new Env(
    config = lila.common.PlayApp loadConfig "simul",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    onGameStart = lila.game.Env.current.onStart,
    isOnline = lila.user.Env.current.isOnline,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
