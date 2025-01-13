package lila.swiss

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.LilaScheduler
import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }
import lila.db.dsl.Coll

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart,
    socketKit: lila.core.socket.SocketKit,
    chat: lila.core.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.core.user.LightUserApi,
    historyApi: lila.core.history.HistoryApi,
    gameProxy: lila.core.game.GameProxy,
    roundApi: lila.core.round.RoundApi,
    mongoCache: lila.memo.MongoCache.Api,
    baseUrl: BaseUrl
)(using
    Executor,
    akka.actor.ActorSystem,
    Scheduler,
    akka.stream.Materializer,
    lila.core.game.IdGenerator,
    play.api.Mode,
    lila.core.user.FlairGet,
    lila.core.i18n.Translator
):

  private val mongo = new SwissMongo(
    swiss = db(CollName("swiss")),
    player = db(CollName("swiss_player")),
    pairing = db(CollName("swiss_pairing")),
    ban = db(CollName("swiss_ban"))
  )

  private val sheetApi = wire[SwissSheetApi]

  private lazy val rankingApi: SwissRankingApi = wire[SwissRankingApi]

  val trf: SwissTrf = wire[SwissTrf]

  private val pairingSystem = PairingSystem(trf, appConfig.get[String]("swiss.bbpairing"))

  private val manualPairing = wire[SwissManualPairing]

  private val scoring = wire[SwissScoring]

  private val director = wire[SwissDirector]

  private val boardApi = wire[SwissBoardApi]

  private val statsApi = wire[SwissStatsApi]

  private val banApi = wire[SwissBanApi]

  lazy val verify = wire[SwissCondition.Verify]

  val api: SwissApi = wire[SwissApi]

  lazy val roundPager = wire[SwissRoundPager]

  private def teamOf = api.teamOf

  private lazy val socket = wire[SwissSocket]

  def version(swissId: SwissId): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](swissId.into(RoomId))(GetVersion.apply)

  lazy val standingApi = wire[SwissStandingApi]

  lazy val json = wire[SwissJson]

  lazy val forms = wire[SwissForm]

  lazy val feature = wire[SwissFeature]

  lazy val cache: SwissCache = wire[SwissCache]

  lazy val getName = GetSwissName(cache.name)

  private lazy val officialSchedule = wire[SwissOfficialSchedule]

  wire[SwissNotify]

  lila.common.Bus.subscribeFun("finishGame", "adjustCheater", "adjustBooster", "team"):
    case lila.core.game.FinishGame(game, _)                    => api.finishGame(game)
    case lila.core.team.LeaveTeam(teamId, userId)              => api.leaveTeam(teamId, userId)
    case lila.core.team.KickFromTeam(teamId, teamName, userId) => api.leaveTeam(teamId, userId)
    case lila.core.mod.MarkCheater(userId, true)               => api.kickLame(userId)
    case lila.core.mod.MarkBooster(userId)                     => api.kickLame(userId)

  LilaScheduler("Swiss.startPendingRounds", _.Every(1.seconds), _.AtMost(20.seconds), _.Delay(20.seconds)):
    api.startPendingRounds.logFailure(logger)

  LilaScheduler("Swiss.checkOngoingGames", _.Every(10.seconds), _.AtMost(15.seconds), _.Delay(20.seconds)):
    api.checkOngoingGames.logFailure(logger)

  LilaScheduler("Swiss.generate", _.Every(3.hours), _.AtMost(15.seconds), _.Delay(15.minutes)):
    officialSchedule.generate.logFailure(logger)

final private class SwissMongo(val swiss: Coll, val player: Coll, val pairing: Coll, val ban: Coll)
