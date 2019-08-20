package lila.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lila.game.{ Game, GameRepo, Pov, PlayerRef }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, Resign, FishnetPlay }
import lila.hub.actorApi.socket.HasUserId
import lila.hub.actorApi.{ Announce, DeployPost }
import lila.user.User

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
    divider: lila.game.Divider,
    prefApi: lila.pref.PrefApi,
    historyApi: lila.history.HistoryApi,
    evalCache: lila.evalCache.EvalCacheApi,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    isBotSync: lila.common.LightUser.IsBotSync,
    slackApi: lila.slack.SlackApi,
    ratingFactors: () => lila.rating.RatingFactors,
    settingStore: lila.memo.SettingStore.Builder
) {

  private val settings = new {
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDuration = config duration "animation.duration"
    val MoretimeDuration = config duration "moretime"
    val SocketTimeout = config duration "socket.timeout"
    val SocketSriTimeout = config duration "socket.sri.timeout"
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

  val persistenceSpeedSetting = settingStore[Int](
    "persistenceSpeed",
    default = -1,
    text = "Force round persistence of games which speed is higher than".some
  )

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
  val roundMap = new lila.hub.DuctMap[RoundDuct](
    mkDuct = id => {
      val duct = new RoundDuct(
        dependencies = roundDependencies,
        gameId = id
      )(new GameProxy(id, deployPersistence.isEnabled, persistenceSpeedSetting.get, system.scheduler))
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
      case lila.hub.actorApi.security.CloseAccount(userId) => GameRepo.allPlaying(userId) map {
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
    bus.publish(lila.hub.actorApi.round.NbRounds(nbRounds), 'nbRounds)
  }

  object proxy {

    def game(gameId: Game.ID): Fu[Option[Game]] = Game.validId(gameId) ??
      roundMap.getOrMake(gameId).getGame addEffect { g =>
        if (!g.isDefined) roundMap kill gameId
      }

    def pov(gameId: Game.ID, user: lila.user.User): Fu[Option[Pov]] =
      game(gameId) map { _ flatMap { Pov(_, user) } }

    def pov(gameId: Game.ID, color: chess.Color): Fu[Option[Pov]] =
      game(gameId) map2 { (g: Game) => Pov(g, color) }

    def pov(fullId: Game.ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

    def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
      game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

    def updateIfPresent(game: Game): Fu[Game] =
      if (game.finishedOrAborted) fuccess(game)
      else roundMap.getIfPresent(game.id).fold(fuccess(game))(_.getGame.map(_ | game))

    def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] =
      roundMap.getIfPresent(gameId).??(_.getGame)

    def urgentGames(user: User): Fu[List[Pov]] = GameRepo urgentPovsUnsorted user flatMap {
      _.map { pov =>
        gameIfPresent(pov.gameId) map { _.fold(pov)(pov.withGame) }
      }.sequenceFu map { povs =>
        try { povs sortWith Pov.priority }
        catch { case e: IllegalArgumentException => povs sortBy (-_.game.movedAt.getSeconds) }
      }
    }
  }

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
      sriTtl = SocketSriTimeout,
      disconnectTimeout = PlayerDisconnectTimeout,
      ragequitTimeout = PlayerRagequitTimeout,
      getGame = proxy.game _
    )
  )

  lazy val selfReport = new SelfReport(roundMap, slackApi, proxy.pov)

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
      bus.publish(lila.game.actorApi.StartGame(game), 'startGame)
      game.userIds foreach { userId =>
        bus.publish(lila.game.actorApi.UserStartGame(userId, game), Symbol(s"userStartGame:$userId"))
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
    divider = lila.game.Env.current.divider,
    prefApi = lila.pref.Env.current.api,
    historyApi = lila.history.Env.current.api,
    evalCache = lila.evalCache.Env.current.api,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    isBotSync = lila.user.Env.current.lightUserApi.isBotSync,
    slackApi = lila.slack.Env.current.api,
    ratingFactors = lila.rating.Env.current.ratingFactorsSetting.get,
    settingStore = lila.memo.Env.current.settingStore
  )
}
