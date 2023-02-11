package lila.swiss

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*
import lila.common.LilaScheduler
import lila.socket.{ GetVersion, SocketVersion }
import lila.db.dsl.Coll

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    historyApi: lila.history.HistoryApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    roundSocket: lila.round.RoundSocket,
    mongoCache: lila.memo.MongoCache.Api,
    baseUrl: lila.common.config.BaseUrl
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    scheduler: Scheduler,
    mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator,
    mode: play.api.Mode
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

  private val pairingSystem = new PairingSystem(trf, rankingApi, appConfig.get[String]("swiss.bbpairing"))

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
    socket.rooms.ask[SocketVersion](swissId into RoomId)(GetVersion.apply)

  lazy val standingApi = wire[SwissStandingApi]

  lazy val json = wire[SwissJson]

  lazy val forms = wire[SwissForm]

  lazy val feature = wire[SwissFeature]

  lazy val cache: SwissCache = wire[SwissCache]

  lazy val getName = new GetSwissName(cache.name)

  private lazy val officialSchedule = wire[SwissOfficialSchedule]

  wire[SwissNotify]

  lila.common.Bus.subscribeFun(
    "finishGame",
    "adjustCheater",
    "adjustBooster",
    "teamLeave"
  ) {
    case lila.game.actorApi.FinishGame(game, _, _)        => api.finishGame(game).unit
    case lila.hub.actorApi.team.LeaveTeam(teamId, userId) => api.leaveTeam(teamId, userId).unit
    case lila.hub.actorApi.mod.MarkCheater(userId, true)  => api.kickLame(userId).unit
    case lila.hub.actorApi.mod.MarkBooster(userId)        => api.kickLame(userId).unit
  }

  LilaScheduler("Swiss.startPendingRounds", _.Every(1 seconds), _.AtMost(20 seconds), _.Delay(20 seconds))(
    api.startPendingRounds
  )

  LilaScheduler("Swiss.checkOngoingGames", _.Every(10 seconds), _.AtMost(15 seconds), _.Delay(20 seconds))(
    api.checkOngoingGames
  )

  LilaScheduler("Swiss.generate", _.Every(3 hours), _.AtMost(15 seconds), _.Delay(15 minutes))(
    officialSchedule.generate
  )

final private class SwissMongo(val swiss: Coll, val player: Coll, val pairing: Coll, val ban: Coll)
