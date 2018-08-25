package lila.round

import akka.actor._
import akka.pattern.{ ask, pipe }
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.{ Tell, Exists, Ask }
import lila.hub.actorApi.round.{ Abort, Resign, FishnetPlay }
import lila.hub.actorApi.{ HasUserId, DeployPost }

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
    isBotSync: lila.common.LightUser.IsBotSync
) {

  private val settings = new {
    val UidTimeout = config duration "uid.timeout"
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDuration = config duration "animation.duration"
    val MoretimeDuration = config duration "moretime"
    val SocketName = config getString "socket.name"
    val SocketTimeout = config duration "socket.timeout"
    val NetDomain = config getString "net.domain"
    val ActiveTtl = config duration "active.ttl"
    val CollectionNote = config getString "collection.note"
    val CollectionHistory = config getString "collection.history"
    val CollectionForecast = config getString "collection.forecast"
    val CollectionAlarm = config getString "collection.alarm"
    val ChannelMoveTime = config getString "channel.move_time.name "
  }
  import settings._

  private val bus = system.lilaBus

  private val moveTimeChannel = system.actorOf(Props(classOf[lila.socket.Channel]), name = ChannelMoveTime)

  lazy val eventHistory = History(db(CollectionHistory)) _

  private lazy val roundDependencies = Round.Dependencies(
    messenger = messenger,
    takebacker = takebacker,
    finisher = finisher,
    rematcher = rematcher,
    player = player,
    drawer = drawer,
    forecastApi = forecastApi,
    socketHub = socketHub,
    scheduler = system.scheduler,
    moretimeDuration = MoretimeDuration
  )
  val roundMap = new lila.hub.DuctMap[Round](
    mkDuct = id => new Round(
      dependencies = roundDependencies,
      gameId = id
    ),
    accessTimeout = ActiveTtl
  )

  bus.subscribeFun('roundMapTell, 'deploy) {
    case Tell(id, msg) => roundMap.tell(id, msg)
    case DeployPost => roundMap.tellAll(DeployPost)
  }

  private var nbRounds = 0
  def count = nbRounds

  system.scheduler.schedule(5 seconds, 2 seconds) {
    nbRounds = roundMap.size
    bus.publish(lila.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
  }

  def roundProxyGame(gameId: Game.ID): Fu[Option[Game]] =
    roundMap.getOrMake(gameId).game.mon(_.round.proxyGameWatcherTime) addEffect { g =>
      if (!g.isDefined) roundMap kill gameId
      lila.mon.round.proxyGameWatcherCount(g.isDefined.toString)()
    }

  private var historyPersistenceEnabled = false

  private val socketHub = new lila.hub.ActorMapNew(
    mkActor = id => new Socket(
      gameId = id,
      history = eventHistory(id, historyPersistenceEnabled),
      lightUser = lightUser,
      uidTimeout = UidTimeout,
      disconnectTimeout = PlayerDisconnectTimeout,
      ragequitTimeout = PlayerRagequitTimeout,
      simulActor = hub.actor.simul
    ),
    accessTimeout = SocketTimeout,
    name = "round.socket",
    system = system
  )

  private val socketHubActor = system.actorOf(Props(new Actor {
    def receive = {
      case msg @ lila.chat.actorApi.ChatLine(id, line) => socketHub.tell(id.value take 8, msg)
      case msg: lila.game.actorApi.StartGame => socketHub.tell(msg.game.id, msg)
      case _: lila.hub.actorApi.Deploy =>
        logger.warn("Enable history persistence")
        historyPersistenceEnabled = true
        // if the deploy didn't go through, cancel persistence
        system.scheduler.scheduleOnce(10.minutes) {
          logger.warn("Disabling round history persistence!")
          historyPersistenceEnabled = false
        }
      case Tell(id, msg) => socketHub.tell(id, msg)
      case Exists(id) => sender ! socketHub.exists(id)
      case Ask(id, msg) =>
        import makeTimeout.short
        socketHub.getOrMake(id) ? msg pipeTo sender
    }
  }), name = SocketName)
  bus.subscribe(socketHubActor, 'tvSelect, 'startGame, 'deploy)

  lazy val selfReport = new SelfReport(roundMap)

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
    socketHub = socketHub,
    messenger = messenger,
    evalCacheHandler = evalCacheHandler,
    selfReport = selfReport,
    bus = bus,
    isRecentTv = recentTvGames get _
  )

  lazy val perfsUpdater = new PerfsUpdater(historyApi, rankingApi)

  lazy val forecastApi: ForecastApi = new ForecastApi(
    coll = db(CollectionForecast),
    roundMap = roundMap
  )

  private lazy val notifier = new RoundNotifier(
    timeline = hub.actor.timeline,
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
    chat = hub.actor.chat
  )

  def getSocketStatus(gameId: Game.ID): Fu[SocketStatus] =
    socketHub.ask[SocketStatus](gameId, GetSocketStatus)

  private def isUserPresent(game: Game, userId: lila.user.User.ID): Fu[Boolean] =
    socketHub.ask[Boolean](game.id, HasUserId(userId))

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
    Props(new Titivate(roundMap, hub.actor.bookmark, hub.actor.chat)),
    name = "titivate"
  )

  bus.subscribe(system.actorOf(
    Props(new CorresAlarm(db(CollectionAlarm), socketHub)),
    name = "corres-alarm"
  ), 'moveEventCorres, 'finishGame)

  lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi,
    bus = bus
  )

  val tvBroadcast = system.actorOf(Props(classOf[TvBroadcast]))
  bus.subscribe(tvBroadcast, 'moveEvent, 'changeFeaturedGame)

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
    isBotSync = lila.user.Env.current.lightUserApi.isBotSync
  )
}
