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

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    proxyGame: Game.ID => Fu[Option[Game]],
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    chatApi: lila.chat.ChatApi,
    tellRound: lila.round.TellRound,
    lightUserApi: lila.user.LightUserApi,
    isOnline: User.ID => Boolean,
    onStart: String => Unit,
    historyApi: lila.history.HistoryApi,
    trophyApi: lila.user.TrophyApi,
    notifyApi: lila.notify.NotifyApi,
    remoteSocketApi: lila.socket.RemoteSocket
)(implicit system: ActorSystem) {

  private val config = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private val scheduler = system.scheduler

  lazy val forms = wire[DataForm]

  private lazy val tournamentRepo = new TournamentRepo(db(config.tournamentColl))
  private lazy val pairingRepo = new PairingRepo(db(config.pairingColl))
  private lazy val playerRepo = new PlayerRepo(db(config.playerColl))
  private lazy val leaderboardColl = db(config.leaderboardColl)

  lazy val cached = wire[Cached]

  lazy val verify = wire[Condition.Verify]

  lazy val winners = wire[WinnersApi]

  lazy val statsApi = wire[TournamentStatsApi]

  lazy val shieldApi: TournamentShieldApi = wire[TournamentShieldApi]

  lazy val revolutionApi: RevolutionApi = wire[RevolutionApi]

  private val duelStore = wire[DuelStore]

  private val pause = wire[Pause]

  private val socket = wire[TournamentSocket]

  private lazy val clearTrophyCache = (tour: Tournament) => {
    if (tour.isShield) scheduler.scheduleOnce(10 seconds)(shieldApi.clear)
    else if (Revolution is tour) scheduler.scheduleOnce(10 seconds)(revolutionApi.clear)
  }

  lazy val api: TournamentApi = wire[TournamentApi]

  lazy val crudApi = wire[crud.CrudApi]

  val tourAndRanks = api tourAndRanks _

  lazy val jsonView = wire[JsonView]

  lazy val apiJsonView = new ApiJsonView(lightUserApi.async)

  lazy val leaderboardApi = new LeaderboardApi(
    coll = leaderboardColl,
    maxPerPage = MaxPerPage(15)
  )

  private lazy val leaderboardIndexer = new LeaderboardIndexer(
    tournamentRepo = tournamentRepo leaderboardColl = leaderboardColl
  )

  private val sequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu(5 seconds)(system),
    accessTimeout = 10 minutes
  )

  lila.common.Bus.subscribe(
    system.actorOf(Props(new ApiActor(api, leaderboardApi)), name = ApiActorName),
    "finishGame", "adjustCheater", "adjustBooster", "playban"
  )

  system.actorOf(Props(new CreatedOrganizer(
    api = api,
    isOnline = isOnline
  )))

  system.actorOf(Props(new StartedOrganizer(
    api = api,
    socket = socket
  )))

  TournamentScheduler.start(system, api)

  def version(tourId: Tournament.ID): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](tourId)(GetVersion)

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    fuccess(socket.hasUser(tourId, userId)) >>| PairingRepo.isPlaying(tourId, userId)

  def cli = new lila.common.Cli {
    def process = {
      case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        leaderboardIndexer.generateAll inject "Done!"
    }
  }

  private lazy val autoPairing = new AutoPairing(duelStore, onStart)
}
