package lidraughts.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lidraughts.game.{ Game, Pov }
import lidraughts.hub.actorApi.HasUserId
import lidraughts.hub.actorApi.map.{ Ask, Tell }

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
    scheduler: lidraughts.common.Scheduler
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

  private val bus = system.lidraughtsBus

  private val moveTimeChannel = system.actorOf(Props(classOf[lidraughts.socket.Channel]), name = ChannelMoveTime)

  lazy val eventHistory = History(db(CollectionHistory)) _

  val roundMap = system.actorOf(Props(new lidraughts.hub.ActorMap {
    def mkActor(id: String) = new Round(
      gameId = id,
      messenger = messenger,
      takebacker = takebacker,
      finisher = finisher,
      rematcher = rematcher,
      player = player,
      drawer = drawer,
      forecastApi = forecastApi,
      socketHub = socketHub,
      awakeWith = msg => self ! Tell(id, msg),
      moretimeDuration = MoretimeDuration,
      activeTtl = ActiveTtl
    )
    def tell(id: Game.ID)(msg: Any): Unit = self ! Tell(id, msg)
    def receive: Receive = ({
      case actorApi.GetNbRounds =>
        nbRounds = size
        bus.publish(lidraughts.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
    }: Receive) orElse actorMapReceive
  }), name = ActorMapName)

  def roundProxyGame(gameId: String): Fu[Option[Game]] = lidraughts.game.GameRepo game gameId

  def actualRoundProxyGame(gameId: String): Fu[Option[Game]] = {
    import makeTimeout.short
    roundMap ? Ask(gameId, actorApi.GetGame) mapTo manifest[Option[Game]]
    // } recoverWith {
    //   case e: akka.pattern.AskTimeoutException =>
    //     logger.info(s"roundProxyGame timeout https://lidraughts.org/$gameId")
    //     lidraughts.game.GameRepo game gameId
  }

  private var nbRounds = 0
  def count() = nbRounds

  private val socketHub = {
    val actor = system.actorOf(
      Props(new lidraughts.socket.SocketHubActor[Socket] {
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
          case msg @ lidraughts.chat.actorApi.ChatLine(id, line) =>
            self ! Tell(id.value take 8, msg)
          case _: lidraughts.hub.actorApi.Deploy =>
            logger.warn("Enable history persistence")
            historyPersistenceEnabled = true
            // if the deploy didn't go through, cancel persistence
            system.scheduler.scheduleOnce(10.minutes) {
              logger.warn("Disabling round history persistence!")
              historyPersistenceEnabled = false
            }
          case msg: lidraughts.game.actorApi.StartGame =>
            self ! Tell(msg.game.id, msg)
        }: Receive) orElse socketHubReceive
      }),
      name = SocketName
    )
    bus.subscribe(actor, 'tvSelect, 'startGame, 'deploy)
    actor
  }

  lazy val selfReport = new SelfReport(roundMap)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    roundMap = roundMap,
    socketHub = socketHub,
    messenger = messenger,
    evalCacheHandler = evalCacheHandler,
    selfReport = selfReport,
    bus = bus
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
    getSocketStatus = getSocketStatus
  )

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    onStart = onStart,
    rematch960Cache = rematch960Cache
  )

  private lazy val player: Player = new Player(
    draughtsnetPlayer = draughtsnetPlayer,
    bus = bus,
    finisher = finisher,
    uciMemo = uciMemo
  )

  private lazy val drawer = new Drawer(
    prefApi = prefApi,
    messenger = messenger,
    finisher = finisher,
    bus = bus
  )

  lazy val messenger = new Messenger(
    chat = hub.actor.chat
  )

  def getSocketStatus(gameId: Game.ID): Fu[SocketStatus] = {
    import makeTimeout.large
    socketHub ? Ask(gameId, GetSocketStatus) mapTo manifest[SocketStatus]
  }

  private def isUserPresent(game: Game, userId: lidraughts.user.User.ID): Fu[Boolean] = {
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

  def resign(pov: Pov): Unit = {
    if (pov.game.abortable)
      roundMap ! Tell(pov.game.id, actorApi.round.Abort(pov.playerId))
    else if (pov.game.playable)
      roundMap ! Tell(pov.game.id, actorApi.round.Resign(pov.playerId))
  }
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
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
