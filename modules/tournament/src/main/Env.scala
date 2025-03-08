package lila.tournament

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import io.lettuce.core.{ RedisClient, RedisURI }
import play.api.Configuration

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    userApi: lila.core.user.UserApi,
    userRepo: lila.core.user.UserRepo,
    gameProxy: lila.core.game.GameProxy,
    chatApi: lila.core.chat.ChatApi,
    roundApi: lila.core.round.RoundApi,
    lightUserApi: lila.core.user.LightUserApi,
    onStart: lila.core.game.OnStart,
    historyApi: lila.core.history.HistoryApi,
    trophyApi: lila.core.user.TrophyApi,
    socketKit: lila.core.socket.SocketKit,
    settingStore: lila.memo.SettingStore.Builder
)(using scheduler: Scheduler)(using
    Executor,
    ActorSystem,
    akka.stream.Materializer,
    lila.core.game.IdGenerator,
    play.api.Mode,
    lila.core.user.FlairGet,
    lila.core.i18n.Translator
):

  lazy val forms = wire[TournamentForm]

  private val playerColl           = CollName("tournament_player")
  lazy val tournamentRepo          = TournamentRepo(db(CollName("tournament2")), playerColl)
  lazy val pairingRepo             = PairingRepo(db(CollName("tournament_pairing")))
  lazy val playerRepo              = PlayerRepo(db(playerColl))
  private lazy val leaderboardRepo = LeaderboardRepo(db(CollName("tournament_leaderboard")))

  lazy val cached: TournamentCache = wire[TournamentCache]

  lazy val verify = wire[TournamentCondition.Verify]

  lazy val winners: WinnersApi = wire[WinnersApi]

  lazy val statsApi = wire[TournamentStatsApi]

  lazy val shieldApi: TournamentShieldApi = wire[TournamentShieldApi]

  lazy val revolutionApi: RevolutionApi = wire[RevolutionApi]

  lazy val moderation = wire[TournamentModeration]

  private lazy val duelStore = wire[DuelStore]

  private lazy val pause = wire[Pause]

  private lazy val waitingUsers = wire[WaitingUsersApi]

  private lazy val socket = wire[TournamentSocket]

  private lazy val pairingSystem = wire[arena.PairingSystem]

  private lazy val apiCallbacks = TournamentApi.Callbacks(
    clearJsonViewCache = jsonView.clearCache,
    clearWinnersCache = winners.clearCache,
    clearTrophyCache = (
        tour =>
          if tour.isShield then scheduler.scheduleOnce(10.seconds) { shieldApi.clear() }
          else if Revolution.is(tour) then scheduler.scheduleOnce(10.seconds) { revolutionApi.clear() }
    ),
    indexLeaderboard = leaderboardIndexer.indexOne
  )

  private lazy val colorHistoryApi = wire[ColorHistoryApi]

  lazy val api: TournamentApi = wire[TournamentApi]

  lazy val coreApi: lila.core.tournament.TournamentApi = new:
    export cached.tourCache.byId as getCached
    export api.{ allCurrentLeadersInStandard, fetchModable }
    export api.gameView.getGameRanks

  lazy val crudApi = wire[crud.CrudApi]

  lazy val crudForm = wire[crud.CrudForm]

  lazy val reloadEndpointSetting = settingStore[String](
    "tournamentReloadEndpoint",
    default = "/tournament/{id}",
    text = "lila-http endpoint. Set to /tournament/{id} to only use lila.".some
  ).taggedWith[TournamentReloadEndpoint]

  lazy val jsonView: JsonView = wire[JsonView]

  lazy val apiJsonView = wire[ApiJsonView]

  lazy val leaderboardApi = wire[LeaderboardApi]

  lazy val standingApi = wire[TournamentStandingApi]

  private lazy val leaderboardIndexer: LeaderboardIndexer = wire[LeaderboardIndexer]

  private lazy val autoPairing = wire[AutoPairing]

  lazy val getTourName = GetTourName(cached.nameCache)

  lazy val featuring = wire[TournamentFeaturing]

  wire[TournamentBusHandler]

  wire[CreatedOrganizer]

  wire[StartedOrganizer]

  wire[TournamentNotify]

  wire[TournamentScheduler]

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
    tournamentRepo.countCreated.foreach { lila.mon.tournament.created.update(_) }

  private val redisClient = RedisClient.create(RedisURI.create(appConfig.get[String]("socket.redis.uri")))
  val lilaHttp            = wire[TournamentLilaHttp]

  def version(tourId: TourId): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](tourId.into(RoomId))(GetVersion.apply)

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: TourId, userId: UserId): Fu[Boolean] =
    fuccess(socket.hasUser(tourId, userId)) >>| pairingRepo.isPlaying(tourId, userId)

  def cli =
    new lila.common.Cli:
      def process =
        // case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        //   leaderboardIndexer.generateAll inject "Done!"
        case "tournament" :: "feature" :: id :: Nil =>
          api.toggleFeaturing(TourId(id), true).inject("Done!")
        case "tournament" :: "unfeature" :: id :: Nil =>
          api.toggleFeaturing(TourId(id), false).inject("Done!")
        case "tournament" :: "recompute" :: id :: Nil =>
          api.recomputeEntireTournament(TourId(id)).inject("Done!")

trait TournamentReloadEndpoint
