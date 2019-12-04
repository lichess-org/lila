package lila.tournament

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.game.Game
import lila.hub.{ Duct, DuctMap, TrouperMap }
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User

@Module
private class RoundConfig(
    @ConfigName("collection.tournament") val tournamentColl: CollName,
    @ConfigName("collection.player") val playerColl: CollName,
    @ConfigName("collection.pairing") val pairingColl: CollName,
    @ConfigName("collection.leaderboard") val leaderboardColl: CollName,
    @ConfigName("leaderboard.cache.ttl") val leaderboardCacheTtl: FiniteDuration,
    @ConfigName("api_actor.name") val apiActorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    isOnline: lila.user.IsOnline,
    proxyRepo: lila.round.GameProxyRepo,
    flood: lila.security.Flood,
    renderer: lila.hub.actors.Renderer,
    timeline: lila.hub.actors.Timeline,
    chatApi: lila.chat.ChatApi,
    tellRound: lila.round.TellRound,
    lightUserApi: lila.user.LightUserApi,
    onStart: lila.round.OnStart,
    historyApi: lila.history.HistoryApi,
    trophyApi: lila.user.TrophyApi,
    notifyApi: lila.notify.NotifyApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(implicit
    system: ActorSystem,
    mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator
) {

  private val config = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private def scheduler = system.scheduler

  lazy val forms = wire[DataForm]

  private lazy val tournamentRepo = new TournamentRepo(db(config.tournamentColl))
  lazy val pairingRepo = new PairingRepo(db(config.pairingColl))
  private lazy val playerRepo = new PlayerRepo(db(config.playerColl))
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
      if (tour.isShield) scheduler.scheduleOnce(10 seconds)(shieldApi.clear)
      else if (Revolution is tour) scheduler.scheduleOnce(10 seconds)(revolutionApi.clear)
    },
    indexLeaderboard = leaderboardIndexer.indexOne _
  )

  lazy val api: TournamentApi = wire[TournamentApi]

  lazy val crudApi = wire[crud.CrudApi]

  lazy val jsonView: JsonView = wire[JsonView]

  lazy val apiJsonView = wire[ApiJsonView]

  lazy val leaderboardApi = wire[LeaderboardApi]

  private lazy val leaderboardIndexer: LeaderboardIndexer = wire[LeaderboardIndexer]

  private def sequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu(5 seconds)(system),
    accessTimeout = 10 minutes
  )

  private lazy val autoPairing = wire[AutoPairing]

  lazy val getTourName = new GetTourName(cached.nameCache.sync _)

  lila.common.Bus.subscribe(
    system.actorOf(Props(wire[ApiActor]), name = config.apiActorName),
    "finishGame", "adjustCheater", "adjustBooster", "playban"
  )

  system.actorOf(Props(wire[CreatedOrganizer]))
  system.actorOf(Props(wire[StartedOrganizer]))

  private lazy val schedulerActor = system.actorOf(Props(wire[TournamentScheduler]))
  scheduler.scheduleWithFixedDelay(1 minute, 5 minutes) {
    () => schedulerActor ! TournamentScheduler.ScheduleNow
  }

  def version(tourId: Tournament.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](tourId)(GetVersion)

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    fuccess(socket.hasUser(tourId, userId)) >>| pairingRepo.isPlaying(tourId, userId)

  def cli = new lila.common.Cli {
    def process = {
      case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        leaderboardIndexer.generateAll inject "Done!"
    }
  }
}
