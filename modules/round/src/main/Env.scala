package lila.round

import akka.actor._
import akka.pattern.ask
import com.github.blemale.scaffeine.Cache
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }

import lila.common.Bus
import lila.game.{ Game, GameRepo, Pov, PlayerRef }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, Resign, FishnetPlay }
import lila.hub.actorApi.simul.GetHostIds
import lila.user.User

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    hub: lila.hub.Env,
    chatApi: lila.chat.ChatApi,
    fishnetPlayer: lila.fishnet.Player,
    aiPerfApi: lila.fishnet.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    playban: lila.playban.PlaybanApi,
    lightUser: lila.common.LightUser.Getter,
    userJsonView: lila.user.JsonView,
    gameJsonView: lila.game.JsonView,
    rankingApi: lila.user.RankingApi,
    notifyApi: lila.notify.NotifyApi,
    uciMemo: lila.game.UciMemo,
    rematches: Cache[Game.ID, Game.ID],
    divider: lila.game.Divider,
    prefApi: lila.pref.PrefApi,
    historyApi: lila.history.HistoryApi,
    evalCache: lila.evalCache.EvalCacheApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    isBotSync: lila.common.LightUser.IsBotSync,
    slackApi: lila.slack.SlackApi,
    ratingFactors: () => lila.rating.RatingFactors
) {

  private val settings = new {
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

  private val deployPersistence = new DeployPersistence(system)

  private val defaultGoneWeight = fuccess(1f)
  private def goneWeight(userId: User.ID): Fu[Float] = playban.getRageSit(userId).dmap(_.goneWeight)
  private def goneWeightsFor(game: Game): Fu[(Float, Float)] =
    if (!game.playable || !game.hasClock || game.hasAi) fuccess(1f -> 1f)
    else game.whitePlayer.userId.fold(defaultGoneWeight)(goneWeight) zip
      game.blackPlayer.userId.fold(defaultGoneWeight)(goneWeight)

  lazy val roundSocket = new RoundSocket(
    remoteSocketApi = remoteSocketApi,
    roundDependencies = new RoundDuct.Dependencies(
      messenger = messenger,
      takebacker = takebacker,
      moretimer = moretimer,
      finisher = finisher,
      rematcher = rematcher,
      player = player,
      drawer = drawer,
      forecastApi = forecastApi,
      isSimulHost = userId => Bus.ask[Set[User.ID]]('simulGetHosts)(GetHostIds)(system).dmap(_ contains userId)
    ),
    deployPersistence = deployPersistence,
    scheduleExpiration = scheduleExpiration,
    tournamentActor = hub.tournamentApi,
    selfReport = selfReport,
    messenger = messenger,
    goneWeightsFor = goneWeightsFor,
    system = system
  )

  Bus.subscribeFuns(
    'roundMapTell -> {
      case Tell(id, msg) => tellRound(id, msg)
    },
    'roundMapTellAll -> {
      case msg => roundSocket.rounds.tellAll(msg)
    },
    'accountClose -> {
      case lila.hub.actorApi.security.CloseAccount(userId) => GameRepo.allPlaying(userId) map {
        _ foreach { pov =>
          tellRound(pov.gameId, Resign(pov.playerId))
        }
      }
    },
    'gameStartId -> {
      case Game.Id(gameId) => onStart(gameId)
    }
  )

  def tellRound(gameId: Game.ID, msg: Any): Unit = roundSocket.rounds.tell(gameId, msg)

  object proxy {

    def game(gameId: Game.ID): Fu[Option[Game]] = Game.validId(gameId) ?? roundSocket.getGame(gameId)

    def pov(gameId: Game.ID, user: lila.user.User): Fu[Option[Pov]] =
      game(gameId) map { _ flatMap { Pov(_, user) } }

    def pov(gameId: Game.ID, color: chess.Color): Fu[Option[Pov]] =
      game(gameId) map2 { (g: Game) => Pov(g, color) }

    def pov(fullId: Game.ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

    def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
      game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

    def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] = roundSocket gameIfPresent gameId

    def updateIfPresent(game: Game): Fu[Game] =
      if (game.finishedOrAborted) fuccess(game)
      else roundSocket updateIfPresent game

    def povIfPresent(gameId: Game.ID, color: chess.Color): Fu[Option[Pov]] =
      gameIfPresent(gameId) map2 { (g: Game) => Pov(g, color) }

    def povIfPresent(fullId: Game.ID): Fu[Option[Pov]] = povIfPresent(PlayerRef(fullId))

    def povIfPresent(playerRef: PlayerRef): Fu[Option[Pov]] =
      gameIfPresent(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

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
      tellRound(game.id, actorApi.round.NoStart)
    }
  }

  lazy val selfReport = new SelfReport(tellRound, slackApi, proxy.pov)

  lazy val recentTvGames = new {
    val fast = new lila.memo.ExpireSetMemo(7 minutes)
    val slow = new lila.memo.ExpireSetMemo(2 hours)
    def get(gameId: Game.ID) = fast.get(gameId) || slow.get(gameId)
    def put(game: Game) = {
      GameRepo.setTv(game.id)
      (if (game.speed <= chess.Speed.Bullet) fast else slow) put game.id
    }
  }

  private lazy val botFarming = new BotFarming(crosstableApi, isBotSync)

  lazy val perfsUpdater = new PerfsUpdater(historyApi, rankingApi, botFarming, ratingFactors)

  lazy val forecastApi: ForecastApi = new ForecastApi(
    coll = db(CollectionForecast),
    tellRound = tellRound
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
    getSocketStatus = getSocketStatus,
    isRecentTv = recentTvGames get _
  )

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    onStart = onStart,
    rematches = rematches
  )
  val isOfferingRematch = rematcher.isOffering _

  private lazy val player: Player = new Player(
    fishnetPlayer = fishnetPlayer,
    finisher = finisher,
    scheduleExpiration = scheduleExpiration,
    uciMemo = uciMemo
  )

  private lazy val drawer = new Drawer(
    prefApi = prefApi,
    messenger = messenger,
    finisher = finisher,
    isBotSync = isBotSync
  )

  lazy val messenger = new Messenger(chatApi)

  def getSocketStatus(game: Game): Fu[SocketStatus] =
    roundSocket.rounds.ask[SocketStatus](game.id)(GetSocketStatus)

  private def isUserPresent(game: Game, userId: lila.user.User.ID): Fu[Boolean] =
    roundSocket.rounds.askIfPresentOrZero[Boolean](game.id)(RoundDuct.HasUserId(userId, _))

  lazy val jsonView = new JsonView(
    noteApi = noteApi,
    userJsonView = userJsonView,
    gameJsonView = gameJsonView,
    getSocketStatus = getSocketStatus,
    canTakeback = takebacker.isAllowedIn,
    canMoretime = moretimer.isAllowedIn,
    divider = divider,
    evalCache = evalCache,
    isOfferingRematch = rematcher.isOffering,
    baseAnimationDuration = AnimationDuration,
    moretimeSeconds = MoretimeDuration.toSeconds.toInt
  )

  lazy val noteApi = new NoteApi(db(CollectionNote))

  def onStart(gameId: Game.ID): Unit = proxy game gameId foreach {
    _ foreach { game =>
      Bus.publish(lila.game.actorApi.StartGame(game), 'startGame)
      game.userIds foreach { userId =>
        Bus.publish(lila.game.actorApi.UserStartGame(userId, game), Symbol(s"userStartGame:$userId"))
      }
    }
  }

  MoveMonitor.start(system)

  system.actorOf(
    Props(new Titivate(tellRound, hub.bookmark, chatApi)),
    name = "titivate"
  )

  private val corresAlarm = new CorresAlarm(system, db(CollectionAlarm), isUserPresent, proxy.game _)

  private lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi
  )
  private lazy val moretimer = new Moretimer(
    messenger = messenger,
    prefApi = prefApi,
    defaultDuration = MoretimeDuration
  )

  val tvBroadcast = system.actorOf(Props(classOf[TvBroadcast]))

  def checkOutoftime(game: Game): Unit = {
    if (game.playable && game.started && !game.isUnlimited)
      tellRound(game.id, actorApi.round.QuietFlag)
  }

  def resign(pov: Pov): Unit =
    if (pov.game.abortable) tellRound(pov.gameId, Abort(pov.playerId))
    else if (pov.game.resignable) tellRound(pov.gameId, Resign(pov.playerId))
}

object Env {

  lazy val current = "round" boot new Env(
    config = lila.common.PlayApp loadConfig "round",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    chatApi = lila.chat.Env.current.api,
    fishnetPlayer = lila.fishnet.Env.current.player,
    aiPerfApi = lila.fishnet.Env.current.aiPerfApi,
    crosstableApi = lila.game.Env.current.crosstableApi,
    playban = lila.playban.Env.current.api,
    lightUser = lila.user.Env.current.lightUser,
    userJsonView = lila.user.Env.current.jsonView,
    gameJsonView = lila.game.Env.current.jsonView,
    rankingApi = lila.user.Env.current.rankingApi,
    notifyApi = lila.notify.Env.current.api,
    uciMemo = lila.game.Env.current.uciMemo,
    rematches = lila.game.Env.current.rematches,
    divider = lila.game.Env.current.divider,
    prefApi = lila.pref.Env.current.api,
    historyApi = lila.history.Env.current.api,
    evalCache = lila.evalCache.Env.current.api,
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    isBotSync = lila.user.Env.current.lightUserApi.isBotSync,
    slackApi = lila.slack.Env.current.api,
    ratingFactors = lila.rating.Env.current.ratingFactorsSetting.get
  )
}
