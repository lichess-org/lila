package lila.round

import actorApi.{ GetSocketStatus, SocketStatus }
import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.common.{ Bus, Uptime }
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.round.{ Abort, Resign }
import lila.hub.actorApi.simul.GetHostIds
import lila.hub.actors
import lila.memo.SettingStore
import scala.util.matching.Regex

@Module
private class RoundConfig(
    @ConfigName("collection.note") val noteColl: CollName,
    @ConfigName("collection.forecast") val forecastColl: CollName,
    @ConfigName("collection.alarm") val alarmColl: CollName
)

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: GameRepo,
    idGenerator: lila.game.IdGenerator,
    userRepo: lila.user.UserRepo,
    timeline: actors.Timeline,
    bookmark: actors.Bookmark,
    chatApi: lila.chat.ChatApi,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
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
    historyApi: lila.history.HistoryApi,
    evalCache: lila.evalCache.EvalCacheApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    lightUserApi: lila.user.LightUserApi,
    ircApi: lila.irc.IrcApi,
    settingStore: lila.memo.SettingStore.Builder,
    ratingFactors: () => lila.rating.RatingFactors,
    notifyColls: lila.notify.NotifyColls,
    shutdown: akka.actor.CoordinatedShutdown
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    materializer: akka.stream.Materializer
):
  private val (botSync, async, sync) = (lightUserApi.isBotSync, lightUserApi.async, lightUserApi.sync)

  private val config = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private val defaultGoneWeight                     = fuccess(1f)
  private def goneWeight(userId: UserId): Fu[Float] = playban.getRageSit(userId).dmap(_.goneWeight)
  private val goneWeightsFor = (game: Game) =>
    if (!game.playable || !game.hasClock || game.hasAi || !Uptime.startedSinceMinutes(1))
      fuccess(1f -> 1f)
    else
      game.whitePlayer.userId.fold(defaultGoneWeight)(goneWeight) zip
        game.blackPlayer.userId.fold(defaultGoneWeight)(goneWeight)

  private val isSimulHost =
    IsSimulHost(userId => Bus.ask[Set[UserId]]("simulGetHosts")(GetHostIds.apply).dmap(_ contains userId))

  private val scheduleExpiration = ScheduleExpiration { game =>
    game.timeBeforeExpiration foreach { centis =>
      scheduler.scheduleOnce((centis.millis + 1000).millis) {
        tellRound(game.id, actorApi.round.NoStart)
      }
    }
  }

  private lazy val proxyDependencies = GameProxy.Dependencies(gameRepo, scheduler)
  private lazy val roundDependencies = wire[RoundAsyncActor.Dependencies]

  lazy val roundSocket: RoundSocket = wire[RoundSocket]

  private def resignAllGamesOf(userId: UserId) =
    gameRepo allPlaying userId foreach {
      _ foreach { pov => tellRound(pov.gameId, Resign(pov.playerId)) }
    }

  Bus.subscribeFuns(
    "accountClose" -> { case lila.hub.actorApi.security.CloseAccount(userId) =>
      resignAllGamesOf(userId)
    },
    "gameStartId" -> { case Game.OnStart(gameId) =>
      onStart(gameId)
    },
    "selfReport" -> { case RoundSocket.Protocol.In.SelfReport(fullId, ip, userId, name) =>
      selfReport(userId, ip, fullId, name).unit
    },
    "adjustCheater" -> { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      resignAllGamesOf(userId)
    }
  )

  lazy val tellRound: TellRound =
    TellRound((gameId: GameId, msg: Any) => roundSocket.rounds.tell(gameId, msg))

  lazy val onStart: OnStart = OnStart((gameId: GameId) =>
    proxyRepo game gameId foreach {
      _ foreach { game =>
        lightUserApi.preloadMany(game.userIds) >>- {
          val sg = lila.game.actorApi.StartGame(game)
          Bus.publish(sg, "startGame")
          game.userIds foreach { userId =>
            Bus.publish(sg, s"userStartGame:$userId")
          }
        }
      }
    }
  )

  lazy val proxyRepo: GameProxyRepo = wire[GameProxyRepo]

  private lazy val correspondenceEmail = wire[CorrespondenceEmail]

  scheduler.scheduleAtFixedRate(10 minute, 10 minute) { (() => correspondenceEmail.tick()) }

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

  private lazy val notifier = RoundNotifier(
    timeline = timeline,
    isUserPresent = isUserPresent,
    notifyApi = notifyApi
  )

  private lazy val finisher = wire[Finisher]

  private lazy val rematcher: Rematcher = wire[Rematcher]

  lazy val isOfferingRematch = IsOfferingRematch(rematcher.isOffering)

  private lazy val player: Player = wire[Player]

  private lazy val drawer = wire[Drawer]

  lazy val messenger = wire[Messenger]

  lazy val getSocketStatus = (game: Game) =>
    roundSocket.rounds.ask[SocketStatus](game.id)(GetSocketStatus.apply)

  private def isUserPresent(game: Game, userId: UserId): Fu[Boolean] =
    roundSocket.rounds.askIfPresentOrZero[Boolean](game.id)(RoundAsyncActor.HasUserId(userId, _))

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = NoteApi(db(config.noteColl))

  MoveLatMonitor.start(scheduler)

  system.actorOf(Props(wire[Titivate]), name = "titivate")

  CorresAlarm(db(config.alarmColl), isUserPresent, proxyRepo.game)

  private lazy val takebacker = wire[Takebacker]

  lazy val moretimer = wire[Moretimer]

  val playing = wire[PlayingUsers]

  val tvBroadcast = system.actorOf(Props(wire[TvBroadcast]))

  val apiMoveStream = wire[ApiMoveStream]

  def resign(pov: Pov): Unit =
    if (pov.game.abortableByUser) tellRound(pov.gameId, Abort(pov.playerId))
    else if (pov.game.resignable) tellRound(pov.gameId, Resign(pov.playerId))

trait SelfReportEndGame
trait SelfReportMarkUser
