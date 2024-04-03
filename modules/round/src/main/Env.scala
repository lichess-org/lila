package lila.round

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import scala.util.matching.Regex

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.common.{ Bus, Uptime }
import lila.game.{ Game, GameRepo, Pov }
import lila.core.round.{ Abort, Resign }
import lila.core.simul.GetHostIds
import lila.memo.SettingStore
import lila.rating.{ PerfType, RatingFactor }

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
    idGenerator: lila.game.IdGenerator,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    userApi: lila.user.UserApi,
    flairApi: lila.user.FlairApi,
    chatApi: lila.chat.ChatApi,
    crosstableApi: lila.game.CrosstableApi,
    playban: lila.playban.PlaybanApi,
    userJsonView: lila.user.JsonView,
    gameJsonView: lila.game.JsonView,
    rankingApi: lila.user.RankingApi,
    notifyApi: lila.notify.NotifyApi,
    uciMemo: lila.game.UciMemo,
    rematches: lila.game.Rematches,
    divider: lila.game.Divider,
    prefApi: lila.pref.PrefApi,
    socketKit: lila.core.socket.ParallelSocketKit,
    userLagPut: lila.core.socket.userLag.Put,
    lightUserApi: lila.user.LightUserApi,
    settingStore: lila.memo.SettingStore.Builder,
    notifyColls: lila.notify.NotifyColls,
    shutdown: akka.actor.CoordinatedShutdown
)(using system: ActorSystem, scheduler: Scheduler)(using
    Executor,
    akka.stream.Materializer,
    lila.core.i18n.Translator
):

  private val (botSync, async, sync) = (lightUserApi.isBotSync, lightUserApi.async, lightUserApi.sync)

  private val config = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private val defaultGoneWeight = fuccess(1f)
  private val goneWeightsFor: Game => Fu[(Float, Float)] = (game: Game) =>
    if !game.playable || !game.hasClock || game.hasAi || !Uptime.startedSinceMinutes(1) then fuccess(1f -> 1f)
    else
      def of(color: chess.Color): Fu[Float] =
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

  private val isSimulHost =
    IsSimulHost(userId => Bus.ask[Set[UserId]]("simulGetHosts")(GetHostIds.apply).dmap(_ contains userId))

  private val scheduleExpiration = ScheduleExpiration: game =>
    game.timeBeforeExpiration.foreach: centis =>
      scheduler.scheduleOnce((centis.millis + 1000).millis):
        tellRound(game.id, lila.core.round.NoStart)

  private lazy val proxyDependencies = wire[GameProxy.Dependencies]
  private lazy val roundDependencies = wire[RoundAsyncActor.Dependencies]

  private given lila.user.FlairApi.Getter = flairApi.getter
  lazy val roundSocket: RoundSocket       = wire[RoundSocket]

  private def resignAllGamesOf(userId: UserId) =
    gameRepo
      .allPlaying(userId)
      .foreach:
        _.foreach { pov => tellRound(pov.gameId, Resign(pov.playerId)) }

  lazy val ratingFactorsSetting =
    import play.api.data.Form
    import play.api.data.Forms.{ single, text }
    import lila.memo.SettingStore.{ Formable, StringReader }
    import lila.rating.{ RatingFactor, RatingFactors }
    import lila.rating.RatingFactor.given
    given StringReader[RatingFactors] = StringReader.fromIso
    given Formable[RatingFactors] = Formable(rfs => Form(single("v" -> text)).fill(RatingFactor.write(rfs)))
    settingStore[lila.rating.RatingFactors](
      "ratingFactor",
      default = Map.empty,
      text = "Rating gain factor per perf type".some
    )
  private val getFactors: () => Map[PerfType, RatingFactor] = ratingFactorsSetting.get

  Bus.subscribeFuns(
    "accountClose" -> { case lila.core.actorApi.security.CloseAccount(userId) =>
      resignAllGamesOf(userId)
    },
    "gameStartId" -> { case Game.OnStart(gameId) =>
      onStart(gameId)
    },
    "selfReport" -> { case RoundSocket.Protocol.In.SelfReport(fullId, ip, userId, name) =>
      selfReport(userId, ip, fullId, name)
    },
    "adjustCheater" -> { case lila.core.mod.MarkCheater(userId, true) =>
      resignAllGamesOf(userId)
    }
  )

  lazy val tellRound: TellRound =
    TellRound((gameId: GameId, msg: Any) => roundSocket.rounds.tell(gameId, msg))

  lazy val onStart = lila.core.game.OnStart: gameId =>
    proxyRepo
      .game(gameId)
      .foreach:
        _.foreach: game =>
          lightUserApi
            .preloadMany(game.userIds)
            .andDo:
              val sg = lila.game.actorApi.StartGame(game)
              Bus.publish(sg, "startGame")
              game.userIds.foreach: userId =>
                Bus.publish(sg, s"userStartGame:$userId")
              if game.playableByAi then Bus.publish(game, "fishnetPlay")

  lazy val proxyRepo: GameProxyRepo = wire[GameProxyRepo]

  private lazy val correspondenceEmail = wire[CorrespondenceEmail]

  scheduler.scheduleAtFixedRate(10 minute, 10 minute): () =>
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

  private lazy val botFarming = wire[BotFarming]

  lazy val perfsUpdater: PerfsUpdater = wire[PerfsUpdater]

  lazy val forecastApi: ForecastApi = ForecastApi(
    coll = db(config.forecastColl),
    tellRound = tellRound
  )

  private lazy val notifier = RoundNotifier(isUserPresent, notifyApi)

  private lazy val finisher = wire[Finisher]

  private lazy val rematcher: Rematcher = wire[Rematcher]

  lazy val isOfferingRematch = lila.core.round.IsOfferingRematch(rematcher.isOffering)

  private lazy val player: Player = wire[Player]

  private lazy val drawer = wire[Drawer]

  lazy val messenger = wire[Messenger]

  lazy val getSocketStatus: Game => Future[SocketStatus] = (game: Game) =>
    roundSocket.rounds.ask[SocketStatus](game.id)(GetSocketStatus.apply)

  private def isUserPresent(game: Game, userId: UserId): Fu[Boolean] =
    roundSocket.rounds.askIfPresentOrZero[Boolean](game.id)(RoundAsyncActor.HasUserId(userId, _))

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = NoteApi(db(config.noteColl))

  lazy val mobile = wire[RoundMobile]

  MoveLatMonitor.start(scheduler)

  system.actorOf(Props(wire[Titivate]), name = "titivate")

  CorresAlarm(db(config.alarmColl), isUserPresent, proxyRepo.game)

  private lazy val takebacker = wire[Takebacker]

  lazy val moretimer = wire[Moretimer]

  val playing = wire[PlayingUsers]

  val apiMoveStream = wire[ApiMoveStream]

  def resign(pov: Pov): Unit =
    if pov.game.abortableByUser then tellRound(pov.gameId, Abort(pov.playerId))
    else if pov.game.resignable then tellRound(pov.gameId, Resign(pov.playerId))

private trait SelfReportEndGame
private trait SelfReportMarkUser
