package lila.round

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import scala.util.matching.Regex

import lila.common.autoconfig.{ *, given }
import lila.common.{ Bus, Uptime }
import lila.core.config.*
import lila.core.round.{ RoundBus, CurrentlyPlaying }
import lila.core.user.FlairGetMap
import lila.game.GameRepo
import lila.memo.SettingStore
import lila.round.RoundGame.*

@Module
private class RoundConfig(
    @ConfigName("collection.note") val noteColl: CollName,
    @ConfigName("collection.forecast") val forecastColl: CollName,
    @ConfigName("collection.alarm") val alarmColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: GameRepo,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    chatApi: lila.chat.ChatApi,
    crosstableApi: lila.game.CrosstableApi,
    playban: lila.playban.PlaybanApi,
    userJsonView: lila.user.JsonView,
    gameJsonView: lila.game.JsonView,
    gameCache: lila.game.Cached,
    rankingApi: lila.user.RankingApi,
    notifyApi: lila.core.notify.NotifyApi,
    uciMemo: lila.game.UciMemo,
    rematches: lila.game.Rematches,
    divider: lila.game.Divider,
    prefApi: lila.pref.PrefApi,
    socketKit: lila.core.socket.ParallelSocketKit,
    userLagPut: lila.core.socket.userLag.Put,
    lightUserApi: lila.user.LightUserApi,
    bookmarkExists: lila.core.misc.BookmarkExists,
    simulApiCircularDep: => lila.core.simul.SimulApi,
    tourApiCircularDep: => lila.core.tournament.TournamentApi,
    settingStore: lila.memo.SettingStore.Builder,
    shutdown: akka.actor.CoordinatedShutdown
)(using system: ActorSystem, scheduler: Scheduler)(using
    FlairGetMap,
    Executor,
    akka.stream.Materializer,
    lila.core.i18n.Translator,
    lila.core.config.RateLimit,
    lila.game.IdGenerator
):

  private val (botSync, async) = (lightUserApi.isBotSync, lightUserApi.async)

  private val config = appConfig.get[RoundConfig]("round")(using AutoConfig.loader)

  private val defaultGoneWeight = fuccess(1f)
  private val goneWeightsFor: Game => Fu[(Float, Float)] = (game: Game) =>
    if !game.playable || !game.hasClock || game.hasAi || !Uptime.startedSinceMinutes(1) then fuccess(1f -> 1f)
    else
      def of(color: Color): Fu[Float] =
        def rageSitGoneWeight(sit: lila.core.playban.RageSit): Float =
          import scala.math.{ log10, sqrt }
          import lila.playban.RageSit.extensions.*
          if !sit.isBad then 1f
          else (1 - 0.7 * sqrt(log10(-(sit.counterView) - 3))).toFloat.max(0.1f)
        game
          .player(color)
          .userId
          .fold(defaultGoneWeight)(uid => playban.rageSitOf(uid).dmap(rageSitGoneWeight))
      of(chess.White).zip(of(chess.Black))

  private val scheduleExpiration = ScheduleExpiration: game =>
    game.timeBeforeExpiration.foreach: centis =>
      scheduler.scheduleOnce((centis.millis + 1000).millis):
        roundApi.tell(game.id, lila.core.round.NoStart)

  private val simulApi = lila.core.data.CircularDep(() => simulApiCircularDep)
  private val tourApi = lila.core.data.CircularDep(() => tourApiCircularDep)

  private lazy val proxyDependencies = wire[GameProxy.Dependencies]
  private lazy val roundDependencies = wire[RoundAsyncActor.Dependencies]

  lazy val roundSocket: RoundSocket = wire[RoundSocket]

  Bus.sub[lila.core.game.GameStart]: game =>
    onStart.exec(game.id)

  Bus.sub[RoundSocket.Protocol.In.SelfReport]:
    case RoundSocket.Protocol.In.SelfReport(fullId, ip, userId, name) =>
      selfReport(userId, ip, fullId, name)

  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) =>
      roundApi.resignAllGamesOf(userId)

  lazy val onStart = lila.core.game.OnStart: gameId =>
    proxyRepo
      .game(gameId)
      .foreach:
        _.foreach: game =>
          for _ <- lightUserApi.preloadMany(game.userIds)
          yield
            val sg = lila.core.game.StartGame(game)
            Bus.pub(sg)
            game.userIds.foreach: userId =>
              Bus.publishDyn(sg, s"userStartGame:$userId")
            if game.playableByAi then Bus.pub(lila.core.fishnet.FishnetMoveRequest(game))

  lazy val proxyRepo: GameProxyRepo = wire[GameProxyRepo]

  lazy val currentlyPlaying = CurrentlyPlaying: userId =>
    gameCache.lastPlayedPlayingId(userId).flatMapz(proxyRepo.pov(_, userId))

  def lastPlayed(userId: UserId): Fu[Option[Pov]] =
    gameRepo.lastPlayed(userId).flatMap(_.traverse(proxyRepo.upgradeIfPresent))

  private lazy val correspondenceEmail = wire[CorrespondenceEmail]

  scheduler.scheduleAtFixedRate(10.minute, 10.minute): () =>
    correspondenceEmail.tick()

  import SettingStore.Regex.given
  val selfReportEndGame = settingStore[Regex](
    "selfReportEndGame",
    default = "-".r,
    text = "Self reports that end the game".some
  ).taggedWith[SelfReportEndGame]

  val selfReportMarkUser = settingStore[Regex](
    "selfReportMarkUser",
    default = "-".r,
    text = "Self reports that mark the user".some
  ).taggedWith[SelfReportMarkUser]

  lazy val selfReport = wire[SelfReport]

  lazy val recentTvGames = wire[RecentTvGames]

  private lazy val farmBoostDetection = wire[FarmBoostDetection]

  lazy val perfsUpdater: PerfsUpdater = wire[PerfsUpdater]

  lazy val forecastApi: ForecastApi = ForecastApi(coll = db(config.forecastColl), roundApi = roundApi)

  private lazy val notifier = RoundNotifier(isUserPresent, notifyApi)

  private lazy val finisher = wire[Finisher]

  private lazy val rematcher: Rematcher = wire[Rematcher]

  lazy val isOfferingRematch = lila.core.round.IsOfferingRematch(rematcher.isOffering)

  private lazy val player: MovePlayer = wire[MovePlayer]

  private lazy val drawer = wire[Drawer]

  lazy val messenger = wire[Messenger]

  lazy val getSocketStatus: Game => Future[SocketStatus] = (game: Game) =>
    roundSocket.rounds.ask[SocketStatus](game.id)(GetSocketStatus.apply)

  private def isUserPresent(game: Game, userId: UserId): Fu[Boolean] =
    roundSocket.rounds.askIfPresentOrZero[Boolean](game.id)(RoundAsyncActor.HasUserId(userId, _))

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = NoteApi(db(config.noteColl))

  lazy val mobile = wire[RoundMobile]

  private lazy val takebacker = wire[Takebacker]

  lazy val moretimer = wire[Moretimer]

  val playing = wire[PlayingUsers]

  val apiMoveStream = wire[ApiMoveStream]

  wire[RoundCourtesy]

  // core APIs
  @annotation.nowarn
  val gameProxy: lila.core.game.GameProxy = new:
    export proxyRepo.{ game, pov, gameIfPresent, updateIfPresent, flushIfPresent, upgradeIfPresent }
  val roundJson: lila.core.round.RoundJson = new:
    export mobile.offline as mobileOffline

  val roundApi: lila.core.round.RoundApi = new:
    export roundSocket.rounds.{ tell, ask }
    export roundSocket.getGames
    def resignAllGamesOf(userId: UserId) =
      gameRepo
        .allPlaying(userId)
        .map:
          _.foreach { pov => roundApi.tell(pov.gameId, RoundBus.Resign(pov.playerId)) }

  val onTvGame: lila.game.core.OnTvGame = recentTvGames.put

  MoveLatMonitor.start(scheduler)

  CorresAlarm(db(config.alarmColl), isUserPresent, proxyRepo.game, lightUserApi)

  system.actorOf(Props(wire[Titivate]), name = "titivate")

  def resign(pov: Pov): Unit =
    if pov.game.abortableByUser then roundApi.tell(pov.gameId, RoundBus.Abort(pov.playerId))
    else if pov.game.resignable then roundApi.tell(pov.gameId, RoundBus.Resign(pov.playerId))

private trait SelfReportEndGame
private trait SelfReportMarkUser
