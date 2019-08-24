package lidraughts.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._
import com.github.blemale.scaffeine.Cache

import actorApi.{ GetSocketStatus, SocketStatus }

import lidraughts.game.{ Game, GameRepo, Pov, PlayerRef }
import lidraughts.hub.actorApi.map.Tell
import lidraughts.hub.actorApi.round.{ Abort, Resign, AnalysisComplete }
import lidraughts.hub.actorApi.socket.HasUserId
import lidraughts.hub.actorApi.{ Announce, DeployPost }
import lidraughts.user.User

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
    gameJsonView: lidraughts.game.JsonView,
    rankingApi: lidraughts.user.RankingApi,
    notifyApi: lidraughts.notify.NotifyApi,
    uciMemo: lidraughts.game.UciMemo,
    rematches: Cache[Game.ID, Game.ID],
    divider: lidraughts.game.Divider,
    prefApi: lidraughts.pref.PrefApi,
    historyApi: lidraughts.history.HistoryApi,
    evalCache: lidraughts.evalCache.EvalCacheApi,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler,
    isBotSync: lidraughts.common.LightUser.IsBotSync,
    ratingFactors: () => lidraughts.rating.RatingFactors,
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

  private val bus = system.lidraughtsBus

  private val moveTimeChannel = new lidraughts.socket.Channel(system)
  bus.subscribe(moveTimeChannel, 'roundMoveTimeChannel)

  private val deployPersistence = new DeployPersistence(system)

  private lazy val roundDependencies = RoundDuct.Dependencies(
    messenger = messenger,
    takebacker = takebacker,
    moretimer = moretimer,
    finisher = finisher,
    rematcher = rematcher,
    player = player,
    drawer = drawer,
    forecastApi = forecastApi,
    socketMap = socketMap
  )
  val roundMap = new lidraughts.hub.DuctMap[RoundDuct](
    mkDuct = id => {
      val duct = new RoundDuct(
        dependencies = roundDependencies,
        gameId = id
      )(new GameProxy(id, deployPersistence.isEnabled, system.scheduler))
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
      case DeployPost => roundMap tellAll DeployPost
    },
    'accountClose -> {
      case lidraughts.hub.actorApi.security.CloseAccount(userId) => GameRepo.allPlaying(userId) map {
        _ foreach { pov =>
          roundMap.tell(pov.gameId, Resign(pov.playerId))
        }
      }
    },
    'gameStartId -> {
      case gameId: String => onStart(gameId)
    }
  )

  private var nbRounds = 0
  def count = nbRounds

  system.scheduler.schedule(5 seconds, 2 seconds) {
    nbRounds = roundMap.size
    bus.publish(lidraughts.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
  }

  object proxy {

    def game(gameId: Game.ID): Fu[Option[Game]] = Game.validId(gameId) ??
      roundMap.getOrMake(gameId).getGame addEffect { g =>
        if (!g.isDefined) roundMap kill gameId
      }

    def pov(gameId: Game.ID, user: lidraughts.user.User): Fu[Option[Pov]] =
      game(gameId) map { _ flatMap { Pov(_, user) } }

    def pov(gameId: Game.ID, color: draughts.Color): Fu[Option[Pov]] =
      game(gameId) map2 { (g: Game) => Pov(g, color) }

    def pov(fullId: Game.ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

    def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
      game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

    def updateIfPresent(game: Game): Fu[Game] =
      if (game.finishedOrAborted) fuccess(game)
      else roundMap.getIfPresent(game.id).fold(fuccess(game))(_.getGame.map(_ | game))

    def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] =
      roundMap.getIfPresent(gameId).??(_.getGame)

    def povIfPresent(gameId: Game.ID, color: draughts.Color): Fu[Option[Pov]] =
      gameIfPresent(gameId) map2 { (g: Game) => Pov(g, color) }

    def povIfPresent(fullId: Game.ID): Fu[Option[Pov]] = povIfPresent(PlayerRef(fullId))

    def povIfPresent(playerRef: PlayerRef): Fu[Option[Pov]] =
      gameIfPresent(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

    private def unsortedPovs(user: User) = GameRepo urgentPovsUnsorted user flatMap {
      _.map { pov =>
        gameIfPresent(pov.gameId) map { _.fold(pov)(pov.withGame) }
      }.sequenceFu
    }

    def urgentGames(user: User): Fu[List[Pov]] = unsortedPovs(user) map { povs =>
      try { povs sortWith Pov.priority }
      catch { case e: IllegalArgumentException => povs sortBy (-_.game.movedAt.getSeconds) }
    }

    def urgentGamesSeq(user: User): Fu[List[Pov]] = unsortedPovs(user) map { povs =>
      try { povs.sortBy(_.game.metadata.simulPairing.getOrElse(Int.MaxValue)) }
      catch { case e: IllegalArgumentException => povs sortBy (-_.game.movedAt.getSeconds) }
    }
  }

  def setAnalysedIfPresent(gameId: Game.ID) =
    roundMap.tellIfPresent(gameId, AnalysisComplete)

  private def scheduleExpiration(game: Game): Unit = game.timeBeforeExpiration foreach { centis =>
    system.scheduler.scheduleOnce((centis.millis + 1000).millis) {
      roundMap.tell(game.id, actorApi.round.NoStart)
    }
  }

  val socketMap = SocketMap.make(
    makeHistory = History(db(CollectionHistory), deployPersistence.isEnabled _) _,
    socketTimeout = SocketTimeout,
    dependencies = RoundSocket.Dependencies(
      system = system,
      lightUser = lightUser,
      uidTtl = SocketUidTimeout,
      disconnectTimeout = PlayerDisconnectTimeout,
      ragequitTimeout = PlayerRagequitTimeout,
      getGame = proxy.game _
    ),
    playban = playban
  )

  lazy val selfReport = new SelfReport(roundMap, proxy.pov)

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
    rematches = rematches,
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
    gameJsonView = gameJsonView,
    getSocketStatus = getSocketStatus,
    canTakeback = takebacker.isAllowedIn,
    canMoretime = moretimer.isAllowedIn,
    divider = divider,
    evalCache = evalCache,
    baseAnimationDuration = AnimationDuration,
    moretimeSeconds = MoretimeDuration.toSeconds.toInt
  )

  lazy val noteApi = new NoteApi(db(CollectionNote))

  def onStart(gameId: Game.ID): Unit = proxy game gameId foreach {
    _ foreach { game =>
      bus.publish(lidraughts.game.actorApi.StartGame(game), 'startGame)
      game.userIds foreach { userId =>
        bus.publish(lidraughts.game.actorApi.UserStartGame(userId, game), Symbol(s"userStartGame:$userId"))
      }
    }
  }

  MoveMonitor.start(system, moveTimeChannel)

  system.actorOf(
    Props(new Titivate(roundMap, hub.bookmark, hub.chat)),
    name = "titivate"
  )

  private val corresAlarm = new CorresAlarm(system, db(CollectionAlarm), socketMap, proxy.game _)

  private lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi,
    bus = bus
  )
  private lazy val moretimer = new Moretimer(
    messenger = messenger,
    prefApi = prefApi,
    defaultDuration = MoretimeDuration
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
    gameJsonView = lidraughts.game.Env.current.jsonView,
    rankingApi = lidraughts.user.Env.current.rankingApi,
    notifyApi = lidraughts.notify.Env.current.api,
    uciMemo = lidraughts.game.Env.current.uciMemo,
    rematches = lidraughts.game.Env.current.rematches,
    divider = lidraughts.game.Env.current.divider,
    prefApi = lidraughts.pref.Env.current.api,
    historyApi = lidraughts.history.Env.current.api,
    evalCache = lidraughts.evalCache.Env.current.api,
    evalCacheHandler = lidraughts.evalCache.Env.current.socketHandler,
    isBotSync = lidraughts.user.Env.current.lightUserApi.isBotSync,
    ratingFactors = lidraughts.rating.Env.current.ratingFactorsSetting.get,
    socketDebug = lidraughts.socket.Env.current.socketDebugSetting.get
  )
}
