package lidraughts.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lidraughts.game.{ Game, GameRepo, Pov }
import lidraughts.hub.actorApi.{ DeployPost, Announce }
import lidraughts.hub.actorApi.map.Tell
import lidraughts.hub.actorApi.round.{ Abort, Resign, AnalysisComplete }
import lidraughts.hub.actorApi.socket.HasUserId

final class Env(
    config: Config,
    system: ActorSystem,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    draughtsnetPlayer: lidraughts.draughtsnet.Player,
    aiPerfApi: lidraughts.draughtsnet.AiPerfApi,
    crosstableApi: lidraughts.game.CrosstableApi,
    playban: lidraughts.playban.PlaybanApi,
    lightUser: lidraughts.common.LightUser.Getter,
    userJsonView: lidraughts.user.JsonView,
    rankingApi: lidraughts.user.RankingApi,
    notifyApi: lidraughts.notify.NotifyApi,
    uciMemo: lidraughts.game.UciMemo,
    rematch960Cache: lidraughts.memo.ExpireSetMemo,
    onStart: String => Unit,
    divider: lidraughts.game.Divider,
    prefApi: lidraughts.pref.PrefApi,
    historyApi: lidraughts.history.HistoryApi,
    evalCache: lidraughts.evalCache.EvalCacheApi,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler,
    isBotSync: lidraughts.common.LightUser.IsBotSync,
    ratingFactors: () => lidraughts.rating.RatingFactors
) {

  private val settings = new {
    val UidTimeout = config duration "uid.timeout"
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDuration = config duration "animation.duration"
    val MoretimeDuration = config duration "moretime"
    val SocketTimeout = config duration "socket.timeout"
    val NetDomain = config getString "net.domain"
    val ActiveTtl = config duration "active.ttl"
    val CollectionNote = config getString "collection.note"
    val CollectionHistory = config getString "collection.history"
    val CollectionForecast = config getString "collection.forecast"
    val CollectionAlarm = config getString "collection.alarm"
  }
  import settings._

  private val bus = system.lidraughtsBus

  private val moveTimeChannel = new lidraughts.socket.Channel(system)
  bus.subscribe(moveTimeChannel, 'roundMoveTimeChannel)

  private lazy val roundDependencies = Round.Dependencies(
    messenger = messenger,
    takebacker = takebacker,
    finisher = finisher,
    rematcher = rematcher,
    player = player,
    drawer = drawer,
    forecastApi = forecastApi,
    socketMap = socketMap,
    moretimeDuration = MoretimeDuration
  )
  val roundMap = new lidraughts.hub.DuctMap[Round](
    mkDuct = id => {
      val duct = new Round(
        dependencies = roundDependencies,
        gameId = id
      )
      duct.getGame foreach { _ foreach scheduleExpiration }
      duct
    },
    accessTimeout = ActiveTtl
  )

  bus.subscribeFuns(
    'roundMapTell -> {
      case Tell(id, msg) => roundMap.tell(id, msg)
    },
    'deploy -> {
      case DeployPost => roundMap.tellAll(DeployPost)
    },
    'accountClose -> {
      case lidraughts.hub.actorApi.security.CloseAccount(userId) => GameRepo.allPlaying(userId) map {
        _ foreach { pov =>
          roundMap.tell(pov.gameId, Resign(pov.playerId))
        }
      }
    }
  )

  private var nbRounds = 0
  def count = nbRounds

  system.scheduler.schedule(5 seconds, 2 seconds) {
    nbRounds = roundMap.size
    bus.publish(lidraughts.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
  }

  def roundProxyGame(gameId: Game.ID): Fu[Option[Game]] =
    roundMap.getOrMake(gameId).getGame addEffect { g =>
      if (!g.isDefined) roundMap kill gameId
    }

  def setAnalysedIfPresent(gameId: Game.ID) =
    roundMap.tellIfPresent(gameId, AnalysisComplete)

  private def scheduleExpiration(game: Game): Unit = game.timeBeforeExpiration foreach { centis =>
    system.scheduler.scheduleOnce((centis.millis + 1000).millis) {
      roundMap.tell(game.id, actorApi.round.NoStart)
    }
  }

  val socketMap = SocketMap.make(
    makeHistory = History(db(CollectionHistory)) _,
    socketTimeout = SocketTimeout,
    dependencies = RoundSocket.Dependencies(
      system = system,
      lightUser = lightUser,
      uidTtl = UidTimeout,
      disconnectTimeout = PlayerDisconnectTimeout,
      ragequitTimeout = PlayerRagequitTimeout
    )
  )

  lazy val selfReport = new SelfReport(roundMap)

  lazy val recentTvGames = new {
    val fast = new lidraughts.memo.ExpireSetMemo(7 minutes)
    val slow = new lidraughts.memo.ExpireSetMemo(2 hours)
    def get(gameId: Game.ID) = fast.get(gameId) || slow.get(gameId)
    def put(game: Game) = {
      GameRepo.setTv(game.id)
      (if (game.speed <= draughts.Speed.Bullet) fast else slow) put game.id
    }
  }

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    roundMap = roundMap,
    socketMap = socketMap,
    messenger = messenger,
    evalCacheHandler = evalCacheHandler,
    selfReport = selfReport,
    bus = bus,
    isRecentTv = recentTvGames get _
  )

  private lazy val botFarming = new BotFarming(crosstableApi, isBotSync)

  lazy val perfsUpdater = new PerfsUpdater(historyApi, rankingApi, botFarming, ratingFactors)

  lazy val forecastApi: ForecastApi = new ForecastApi(
    coll = db(CollectionForecast),
    roundMap = roundMap
  )

  private lazy val notifier = new RoundNotifier(
    timeline = hub.timeline,
    isUserPresent = isUserPresent,
    notifyApi = notifyApi
  )

  private lazy val finisher = new Finisher(
    messenger = messenger,
    perfsUpdater = perfsUpdater,
    crosstableApi = crosstableApi,
    notifier = notifier,
    playban = playban,
    bus = bus,
    getSocketStatus = getSocketStatus,
    isRecentTv = recentTvGames get _
  )

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    onStart = onStart,
    rematch960Cache = rematch960Cache,
    bus = bus
  )

  private lazy val player: Player = new Player(
    system = system,
    draughtsnetPlayer = draughtsnetPlayer,
    bus = bus,
    finisher = finisher,
    scheduleExpiration = scheduleExpiration,
    uciMemo = uciMemo
  )

  private lazy val drawer = new Drawer(
    prefApi = prefApi,
    messenger = messenger,
    finisher = finisher,
    isBotSync = isBotSync,
    bus = bus
  )

  lazy val messenger = new Messenger(
    chat = hub.chat
  )

  def getSocketStatus(gameId: Game.ID): Fu[SocketStatus] =
    socketMap.ask[SocketStatus](gameId)(GetSocketStatus)

  private def isUserPresent(game: Game, userId: lidraughts.user.User.ID): Fu[Boolean] =
    socketMap.ask[Boolean](game.id)(HasUserId(userId, _))

  lazy val jsonView = new JsonView(
    noteApi = noteApi,
    userJsonView = userJsonView,
    getSocketStatus = getSocketStatus,
    canTakeback = takebacker.isAllowedByPrefs,
    divider = divider,
    evalCache = evalCache,
    baseAnimationDuration = AnimationDuration,
    moretimeSeconds = MoretimeDuration.toSeconds.toInt
  )

  lazy val noteApi = new NoteApi(db(CollectionNote))

  MoveMonitor.start(system, moveTimeChannel)

  system.actorOf(
    Props(new Titivate(roundMap, hub.bookmark, hub.chat)),
    name = "titivate"
  )

  private val corresAlarm = new CorresAlarm(system, db(CollectionAlarm), socketMap)

  lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi,
    bus = bus
  )

  val tvBroadcast = system.actorOf(Props(classOf[TvBroadcast]))

  def checkOutoftime(game: Game): Unit = {
    if (game.playable && game.started && !game.isUnlimited)
      roundMap.tell(game.id, actorApi.round.QuietFlag)
  }

  def resign(pov: Pov): Unit =
    if (pov.game.abortable) roundMap.tell(pov.gameId, Abort(pov.playerId))
    else if (pov.game.resignable) roundMap.tell(pov.gameId, Resign(pov.playerId))
}

object Env {

  lazy val current = "round" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "round",
    system = lidraughts.common.PlayApp.system,
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    draughtsnetPlayer = lidraughts.draughtsnet.Env.current.player,
    aiPerfApi = lidraughts.draughtsnet.Env.current.aiPerfApi,
    crosstableApi = lidraughts.game.Env.current.crosstableApi,
    playban = lidraughts.playban.Env.current.api,
    lightUser = lidraughts.user.Env.current.lightUser,
    userJsonView = lidraughts.user.Env.current.jsonView,
    rankingApi = lidraughts.user.Env.current.rankingApi,
    notifyApi = lidraughts.notify.Env.current.api,
    uciMemo = lidraughts.game.Env.current.uciMemo,
    rematch960Cache = lidraughts.game.Env.current.cached.rematch960,
    onStart = lidraughts.game.Env.current.onStart,
    divider = lidraughts.game.Env.current.divider,
    prefApi = lidraughts.pref.Env.current.api,
    historyApi = lidraughts.history.Env.current.api,
    evalCache = lidraughts.evalCache.Env.current.api,
    evalCacheHandler = lidraughts.evalCache.Env.current.socketHandler,
    isBotSync = lidraughts.user.Env.current.lightUserApi.isBotSync,
    ratingFactors = lidraughts.rating.Env.current.ratingFactorsSetting.get
  )
}
