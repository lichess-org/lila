package lidraughts.simul

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.hub.{ Duct, DuctMap, TrouperMap }
import lidraughts.socket.History
import lidraughts.socket.Socket.{ GetVersion, SocketVersion }

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    evalCacheApi: lidraughts.evalCache.EvalCacheApi,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    draughtsnetCommentator: lidraughts.draughtsnet.Commentator,
    roundMap: DuctMap[_],
    lightUser: lidraughts.common.LightUser.Getter,
    onGameStart: String => Unit,
    isOnline: String => Boolean,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionSimul = config getString "collection.simul"
    val SequencerTimeout = config duration "sequencer.timeout"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val UniqueCacheTtl = config duration "unique.cache.ttl"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
  }
  import settings._

  lazy val repo = new SimulRepo(
    simulColl = simulColl
  )

  lazy val api = new SimulApi(
    repo = repo,
    system = system,
    socketMap = socketMap,
    roundMap = roundMap,
    renderer = hub.renderer,
    timeline = hub.timeline,
    onGameStart = onGameStart,
    sequencers = sequencerMap,
    asyncCache = asyncCache
  )

  def evalCache = evalCacheApi

  lazy val crudApi = new crud.CrudApi(repo)

  lazy val forms = new DataForm

  lazy val jsonView = new JsonView(lightUser, isOnline)

  private val socketMap: SocketMap = lidraughts.socket.SocketMap[Socket](
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

  system.lidraughtsBus.subscribeFuns(
    'finishGame -> {
      case lidraughts.game.actorApi.FinishGame(game, _, _) => api finishGame game
    },
    'adjustCheater -> {
      case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) => api ejectCheater userId
    },
    'deploy -> {
      case m: lidraughts.hub.actorApi.Deploy => socketMap tellAll m
    },
    'simulGetHosts -> {
      case lidraughts.hub.actorApi.simul.GetHostIds(promise) => promise completeWith api.currentHostIds
    },
    'moveEventSimul -> {
      case lidraughts.hub.actorApi.round.SimulMoveEvent(move, simulId, opponentUserId) =>
        system.lidraughtsBus.publish(
          lidraughts.hub.actorApi.socket.SendTo(
            opponentUserId,
            lidraughts.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
          ),
          'socketUsers
        )
        allUniqueWithCommentary.get.foreach { ids =>
          if (ids.contains(simulId))
            draughtsnetCommentator(move.gameId)
        }
    },
    'draughtsnetComment -> {
      case lidraughts.hub.actorApi.draughtsnet.CommentaryEvent(gameId, simulId, json) if simulId.isDefined =>
        allUniqueWithPublicCommentary.get.foreach { ids =>
          api.processCommentary(simulId.get, gameId, json, ids.contains(simulId.get))
        }
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

  val allUniqueFeaturable = asyncCache.single(
    name = "simul.allUniqueFeaturable",
    repo.allUniqueFeaturable,
    expireAfter = _.ExpireAfterWrite(UniqueCacheTtl)
  )

  val allUniqueWithCommentary = asyncCache.single(
    name = "simul.allUniqueWithCommentaryIds",
    repo.allUniqueWithCommentaryIds,
    expireAfter = _.ExpireAfterWrite(UniqueCacheTtl)
  )

  val allUniqueWithPublicCommentary = asyncCache.single(
    name = "simul.allUniqueWithPublicCommentary",
    repo.allUniqueWithPublicCommentaryIds,
    expireAfter = _.ExpireAfterWrite(UniqueCacheTtl)
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
    config = lidraughts.common.PlayApp loadConfig "simul",
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler,
    evalCacheApi = lidraughts.evalCache.Env.current.api,
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    draughtsnetCommentator = lidraughts.draughtsnet.Env.current.commentator,
    roundMap = lidraughts.round.Env.current.roundMap,
    lightUser = lidraughts.user.Env.current.lightUser,
    onGameStart = lidraughts.game.Env.current.onStart,
    isOnline = lidraughts.user.Env.current.isOnline,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
