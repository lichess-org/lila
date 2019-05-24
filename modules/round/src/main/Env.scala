package lila.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.{ DeployPost, Announce }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, Resign, FishnetPlay }
import lila.hub.actorApi.socket.HasUserId

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    hub: lila.hub.Env,
    fishnetPlayer: lila.fishnet.Player,
    aiPerfApi: lila.fishnet.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    playban: lila.playban.PlaybanApi,
    lightUser: lila.common.LightUser.Getter,
    userJsonView: lila.user.JsonView,
    rankingApi: lila.user.RankingApi,
    notifyApi: lila.notify.NotifyApi,
    uciMemo: lila.game.UciMemo,
    rematch960Cache: lila.memo.ExpireSetMemo,
    onStart: String => Unit,
    divider: lila.game.Divider,
    prefApi: lila.pref.PrefApi,
    historyApi: lila.history.HistoryApi,
    evalCache: lila.evalCache.EvalCacheApi,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    isBotSync: lila.common.LightUser.IsBotSync,
    slackApi: lila.slack.SlackApi,
    ratingFactors: () => lila.rating.RatingFactors,
    val socketDebug: () => Boolean
) {

  private val settings = new {
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDuration = config duration "animation.duration"
    val MoretimeDuration = config duration "moretime"
    val SocketTimeout = config duration "socket.timeout"
    val SocketUidTimeout = config duration "socket.uid.timeout"
    val NetDomain = config getString "net.domain"
    val ActiveTtl = config duration "active.ttl"
    val CollectionNote = config getString "collection.note"
    val CollectionHistory = config getString "collection.history"
    val CollectionForecast = config getString "collection.forecast"
    val CollectionAlarm = config getString "collection.alarm"
  }
  import settings._

  private val bus = system.lilaBus

  private val moveTimeChannel = new lila.socket.Channel(system)
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
  val roundMap = new lila.hub.DuctMap[Round](
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
      case lila.hub.actorApi.security.CloseAccount(userId) => GameRepo.allPlaying(userId) map {
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
    bus.publish(lila.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
  }

  def roundProxyGame(gameId: Game.ID): Fu[Option[Game]] =
    roundMap.getOrMake(gameId).getGame addEffect { g =>
      if (!g.isDefined) roundMap kill gameId
    }

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
      uidTtl = SocketUidTimeout,
      disconnectTimeout = PlayerDisconnectTimeout,
      ragequitTimeout = PlayerRagequitTimeout
    )
  )

  lazy val selfReport = new SelfReport(roundMap, slackApi)

  lazy val recentTvGames = new {
    val fast = new lila.memo.ExpireSetMemo(7 minutes)
    val slow = new lila.memo.ExpireSetMemo(2 hours)
    def get(gameId: Game.ID) = fast.get(gameId) || slow.get(gameId)
    def put(game: Game) = {
      GameRepo.setTv(game.id)
      (if (game.speed <= chess.Speed.Bullet) fast else slow) put game.id
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
    fishnetPlayer = fishnetPlayer,
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

  private def isUserPresent(game: Game, userId: lila.user.User.ID): Fu[Boolean] =
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
    config = lila.common.PlayApp loadConfig "round",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    fishnetPlayer = lila.fishnet.Env.current.player,
    aiPerfApi = lila.fishnet.Env.current.aiPerfApi,
    crosstableApi = lila.game.Env.current.crosstableApi,
    playban = lila.playban.Env.current.api,
    lightUser = lila.user.Env.current.lightUser,
    userJsonView = lila.user.Env.current.jsonView,
    rankingApi = lila.user.Env.current.rankingApi,
    notifyApi = lila.notify.Env.current.api,
    uciMemo = lila.game.Env.current.uciMemo,
    rematch960Cache = lila.game.Env.current.cached.rematch960,
    onStart = lila.game.Env.current.onStart,
    divider = lila.game.Env.current.divider,
    prefApi = lila.pref.Env.current.api,
    historyApi = lila.history.Env.current.api,
    evalCache = lila.evalCache.Env.current.api,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    isBotSync = lila.user.Env.current.lightUserApi.isBotSync,
    slackApi = lila.slack.Env.current.api,
    ratingFactors = lila.rating.Env.current.ratingFactorsSetting.get,
    socketDebug = lila.socket.Env.current.socketDebugSetting.get
  )
}
