package lila.tournament

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User

@Module
private class TournamentConfig(
    @ConfigName("collection.tournament") val tournamentColl: CollName,
    @ConfigName("collection.player") val playerColl: CollName,
    @ConfigName("collection.pairing") val pairingColl: CollName,
    @ConfigName("collection.leaderboard") val leaderboardColl: CollName,
    @ConfigName("api_actor.name") val apiActorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    proxyRepo: lila.round.GameProxyRepo,
    renderer: lila.hub.actors.Renderer,
    chatApi: lila.chat.ChatApi,
    tellRound: lila.round.TellRound,
    roundSocket: lila.round.RoundSocket,
    lightUserApi: lila.user.LightUserApi,
    onStart: lila.round.OnStart,
    historyApi: lila.history.HistoryApi,
    trophyApi: lila.user.TrophyApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator,
    mode: play.api.Mode
) {

  private val config = appConfig.get[TournamentConfig]("tournament")(AutoConfig.loader)

  private def scheduler = system.scheduler

  lazy val forms = wire[TournamentForm]

  lazy val tournamentRepo          = new TournamentRepo(db(config.tournamentColl), config.playerColl)
  lazy val pairingRepo             = new PairingRepo(db(config.pairingColl))
  lazy val playerRepo              = new PlayerRepo(db(config.playerColl))
  private lazy val leaderboardRepo = new LeaderboardRepo(db(config.leaderboardColl))

  lazy val cached: Cached = wire[Cached]

  lazy val verify = wire[Condition.Verify]

  lazy val winners: WinnersApi = wire[WinnersApi]

  lazy val statsApi = wire[TournamentStatsApi]

  lazy val shieldApi: TournamentShieldApi = wire[TournamentShieldApi]

  lazy val revolutionApi: RevolutionApi = wire[RevolutionApi]

  private lazy val duelStore = wire[DuelStore]

  private lazy val pause = wire[Pause]

  private lazy val socket = wire[TournamentSocket]

  private lazy val pairingSystem = wire[arena.PairingSystem]

  private lazy val apiCallbacks = TournamentApi.Callbacks(
    clearJsonViewCache = jsonView.clearCache,
    clearWinnersCache = winners.clearCache,
    clearTrophyCache = tour => {
      if (tour.isShield) scheduler.scheduleOnce(10 seconds) { shieldApi.clear() }
      else if (Revolution is tour) scheduler.scheduleOnce(10 seconds) { revolutionApi.clear() }
    },
    indexLeaderboard = leaderboardIndexer.indexOne
  )

  private lazy val colorHistoryApi = wire[ColorHistoryApi]

  lazy val api: TournamentApi = wire[TournamentApi]

  lazy val crudApi = wire[crud.CrudApi]

  lazy val jsonView: JsonView = wire[JsonView]

  lazy val apiJsonView = wire[ApiJsonView]

  lazy val leaderboardApi = wire[LeaderboardApi]

  lazy val standingApi = wire[TournamentStandingApi]

  private lazy val leaderboardIndexer: LeaderboardIndexer = wire[LeaderboardIndexer]

  private lazy val autoPairing = wire[AutoPairing]

  lazy val getTourName = new GetTourName((id, lang) => cached.nameCache.sync(id -> lang))

  system.actorOf(Props(wire[ApiActor]), name = config.apiActorName)

  system.actorOf(Props(wire[CreatedOrganizer]))

  system.actorOf(Props(wire[StartedOrganizer]))

  private lazy val schedulerActor = system.actorOf(Props(wire[TournamentScheduler]))
  scheduler.scheduleWithFixedDelay(1 minute, 5 minutes) { () =>
    schedulerActor ! TournamentScheduler.ScheduleNow
  }

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    tournamentRepo.countCreated foreach { lila.mon.tournament.created.update(_) }
  }

  def version(tourId: Tournament.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](tourId)(GetVersion)

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    fuccess(socket.hasUser(tourId, userId)) >>| pairingRepo.isPlaying(tourId, userId)

  def cli =
    new lila.common.Cli {
      def process = {
        case "tournament" :: "leaderboard" :: "generate" :: Nil =>
          leaderboardIndexer.generateAll inject "Done!"
        case "tournament" :: "feature" :: id :: Nil =>
          api.toggleFeaturing(id, true) inject "Done!"
        case "tournament" :: "unfeature" :: id :: Nil =>
          api.toggleFeaturing(id, false) inject "Done!"
      }
    }
}
