package lila.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.HasUserId
import lila.hub.actorApi.round.{ Abort, Resign }
import lila.hub.actorApi.map.{ Ask, Tell }

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
    scheduler: lila.common.Scheduler
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
    val ActorMapName = config getString "actor.map.name"
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

  val roundMap = system.actorOf(Props(new lila.hub.ActorMap {
    private lazy val dependencies = Round.Dependencies(
      messenger = messenger,
      takebacker = takebacker,
      finisher = finisher,
      rematcher = rematcher,
      player = player,
      drawer = drawer,
      forecastApi = forecastApi,
      socketHub = socketHub,
      moretimeDuration = MoretimeDuration,
      activeTtl = ActiveTtl
    )
    def mkActor(id: String) = new Round(
      dependencies = dependencies,
      gameId = id,
      awakeWith = msg => self ! Tell(id, msg)
    )
    def receive: Receive = ({
      case actorApi.GetNbRounds =>
        nbRounds = size
        bus.publish(lila.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
    }: Receive) orElse actorMapReceive
  }), name = ActorMapName)

  private var nbRounds = 0
  def count() = nbRounds

  def roundProxyGame(gameId: Game.ID): Fu[Option[Game]] = {
    import makeTimeout.halfSecond
    roundMap ? Ask(gameId, actorApi.GetGame) mapTo manifest[Fu[Option[Game]]]
  }.flatMap(identity).mon(_.round.proxyGameWatcherTime) addEffect { g =>
    lila.mon.round.proxyGameWatcherCount(g.isDefined.toString)()
  } recoverWith {
    case e: akka.pattern.AskTimeoutException =>
      // weird. monitor and try again.
      lila.mon.round.proxyGameWatcherCount("exception")()
      import makeTimeout.halfSecond
      roundMap ? Ask(gameId, actorApi.GetGame) mapTo manifest[Fu[Option[Game]]] flatMap identity recoverWith {
        case e: akka.pattern.AskTimeoutException =>
          // again? monitor, log and fallback on DB
          lila.mon.round.proxyGameWatcherCount("double_exception")()
          logger.warn(s"roundProxyGame double timeout https://lichess.org/$gameId")
          lila.game.GameRepo game gameId
      }
  }

  private val socketHub = {
    val actor = system.actorOf(
      Props(new lila.socket.SocketHubActor[Socket] {
        private var historyPersistenceEnabled = false
        def mkActor(id: String) = new Socket(
          gameId = id,
          history = eventHistory(id, historyPersistenceEnabled),
          lightUser = lightUser,
          uidTimeout = UidTimeout,
          socketTimeout = SocketTimeout,
          disconnectTimeout = PlayerDisconnectTimeout,
          ragequitTimeout = PlayerRagequitTimeout,
          simulActor = hub.actor.simul
        )
        def receive: Receive = ({
          case msg @ lila.chat.actorApi.ChatLine(id, line) =>
            self ! Tell(id.value take 8, msg)
          case _: lila.hub.actorApi.Deploy =>
            logger.warn("Enable history persistence")
            historyPersistenceEnabled = true
            // if the deploy didn't go through, cancel persistence
            system.scheduler.scheduleOnce(10.minutes) {
              logger.warn("Disabling round history persistence!")
              historyPersistenceEnabled = false
            }
          case msg: lila.game.actorApi.StartGame =>
            self ! Tell(msg.game.id, msg)
        }: Receive) orElse socketHubReceive
      }),
      name = SocketName
    )
    bus.subscribe(actor, 'tvSelect, 'startGame, 'deploy)
    actor
  }

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
    roundMap = hub.actor.roundMap
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

  def getSocketStatus(gameId: Game.ID): Fu[SocketStatus] = {
    import makeTimeout.large
    socketHub ? Ask(gameId, GetSocketStatus) mapTo manifest[SocketStatus]
  }

  private def isUserPresent(game: Game, userId: lila.user.User.ID): Fu[Boolean] = {
    import makeTimeout.large
    socketHub ? Ask(game.id, HasUserId(userId)) mapTo manifest[Boolean]
  }

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

  scheduler.message(2.1 seconds)(roundMap -> actorApi.GetNbRounds)

  system.actorOf(
    Props(new Titivate(roundMap, hub.actor.bookmark, hub.actor.chat)),
    name = "titivate"
  )

  bus.subscribe(system.actorOf(
    Props(new CorresAlarm(db(CollectionAlarm), hub.socket.round)),
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
      roundMap ! Tell(game.id, actorApi.round.QuietFlag)
  }

  def resign(pov: Pov): Unit =
    if (pov.game.abortable) roundMap ! Tell(pov.gameId, Abort(pov.playerId))
    else if (pov.game.resignable) roundMap ! Tell(pov.gameId, Resign(pov.playerId))
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
    scheduler = lila.common.PlayApp.scheduler
  )
}
