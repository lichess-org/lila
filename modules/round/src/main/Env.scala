package lila.round

import actorApi.{ GetSocketStatus, SocketStatus }
import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.common.{ Bus, Uptime }
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.round.{ Abort, Resign }
import lila.hub.actorApi.simul.GetHostIds
import lila.hub.actors
import lila.user.User

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
    timeline: actors.Timeline,
    bookmark: actors.Bookmark,
    tournamentApi: actors.TournamentApi,
    chatApi: lila.chat.ChatApi,
    fishnetPlayer: lila.fishnet.Player,
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
    isBotSync: lila.common.LightUser.IsBotSync,
    lightUserSync: lila.common.LightUser.GetterSync,
    slackApi: lila.slack.SlackApi,
    ratingFactors: () => lila.rating.RatingFactors,
    shutdown: akka.actor.CoordinatedShutdown
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    scheduler: akka.actor.Scheduler
) {

  implicit private val animationLoader = durationLoader(AnimationDuration.apply)
  private val config                   = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private val defaultGoneWeight                      = fuccess(1f)
  private def goneWeight(userId: User.ID): Fu[Float] = playban.getRageSit(userId).dmap(_.goneWeight)
  private val goneWeightsFor = (game: Game) =>
    if (!game.playable || !game.hasClock || game.hasAi || !Uptime.startedSinceMinutes(1))
      fuccess(1f -> 1f)
    else
      game.whitePlayer.userId.fold(defaultGoneWeight)(goneWeight) zip
        game.blackPlayer.userId.fold(defaultGoneWeight)(goneWeight)

  private val isSimulHost = new IsSimulHost(userId =>
    Bus.ask[Set[User.ID]]("simulGetHosts")(GetHostIds).dmap(_ contains userId)
  )

  private val scheduleExpiration = new ScheduleExpiration(game => {
    game.timeBeforeExpiration foreach { centis =>
      scheduler.scheduleOnce((centis.millis + 1000).millis) {
        tellRound(game.id, actorApi.round.NoStart)
      }
    }
  })

  private lazy val proxyDependencies =
    new GameProxy.Dependencies(gameRepo, scheduler)
  private lazy val roundDependencies = wire[RoundDuct.Dependencies]

  lazy val roundSocket: RoundSocket = wire[RoundSocket]

  private def resignAllGamesOf(userId: User.ID) =
    gameRepo allPlaying userId foreach {
      _ foreach { pov => tellRound(pov.gameId, Resign(pov.playerId)) }
    }

  Bus.subscribeFuns(
    "accountClose" -> { case lila.hub.actorApi.security.CloseAccount(userId) =>
      resignAllGamesOf(userId)
    },
    "gameStartId" -> { case Game.Id(gameId) =>
      onStart(gameId)
    },
    "selfReport" -> { case RoundSocket.Protocol.In.SelfReport(fullId, ip, userId, name) =>
      selfReport(userId, ip, fullId, name).unit
    },
    "adjustCheater" -> { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      resignAllGamesOf(userId)
    }
  )

  lazy val tellRound: TellRound = new TellRound((gameId: Game.ID, msg: Any) =>
    roundSocket.rounds.tell(gameId, msg)
  )

  lazy val onStart: OnStart = new OnStart((gameId: Game.ID) =>
    proxyRepo game gameId foreach {
      _ foreach { game =>
        Bus.publish(lila.game.actorApi.StartGame(game), "startGame")
        game.userIds foreach { userId =>
          Bus.publish(lila.game.actorApi.StartGame(game), s"userStartGame:$userId")
        }
      }
    }
  )

  lazy val proxyRepo: GameProxyRepo = wire[GameProxyRepo]

  lazy val selfReport = wire[SelfReport]

  lazy val recentTvGames = wire[RecentTvGames]

  private lazy val botFarming = wire[BotFarming]

  lazy val perfsUpdater: PerfsUpdater = wire[PerfsUpdater]

  lazy val forecastApi: ForecastApi = new ForecastApi(
    coll = db(config.forecastColl),
    tellRound = tellRound
  )

  private lazy val notifier = new RoundNotifier(
    timeline = timeline,
    isUserPresent = isUserPresent,
    notifyApi = notifyApi
  )

  private lazy val finisher = wire[Finisher]

  private lazy val rematcher: Rematcher = wire[Rematcher]

  lazy val isOfferingRematch = new IsOfferingRematch(rematcher.isOffering)

  private lazy val player: Player = wire[Player]

  private lazy val drawer = wire[Drawer]

  lazy val messenger = wire[Messenger]

  lazy val getSocketStatus = (game: Game) => roundSocket.rounds.ask[SocketStatus](game.id)(GetSocketStatus)

  private def isUserPresent(game: Game, userId: lila.user.User.ID): Fu[Boolean] =
    roundSocket.rounds.askIfPresentOrZero[Boolean](game.id)(RoundDuct.HasUserId(userId, _))

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = new NoteApi(db(config.noteColl))

  MoveLatMonitor.start(scheduler)

  system.actorOf(Props(wire[Titivate]), name = "titivate")

  new CorresAlarm(db(config.alarmColl), isUserPresent, proxyRepo.game)

  private lazy val takebacker = wire[Takebacker]

  private lazy val moretimer = wire[Moretimer]

  val playing = wire[PlayingUsers]

  val tvBroadcast = system.actorOf(Props(wire[TvBroadcast]))

  def resign(pov: Pov): Unit =
    if (pov.game.abortable) tellRound(pov.gameId, Abort(pov.playerId))
    else if (pov.game.resignable) tellRound(pov.gameId, Resign(pov.playerId))
}
