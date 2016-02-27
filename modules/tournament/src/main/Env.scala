package lila.tournament

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import lila.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMap: ActorRef,
    roundSocketHub: ActorSelection,
    lightUser: String => Option[lila.common.LightUser],
    isOnline: String => Boolean,
    onStart: String => Unit,
    trophyApi: lila.user.TrophyApi,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionTournament = config getString "collection.tournament"
    val CollectionPlayer = config getString "collection.player"
    val CollectionPairing = config getString "collection.pairing"
    val CollectionLeaderboard = config getString "collection.leaderboard"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val LeaderboardCacheTtl = config duration "leaderboard.cache.ttl"
    val RankingCacheTtl = config duration "ranking.cache.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val OrganizerName = config getString "organizer.name"
    val ReminderName = config getString "reminder.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  lazy val forms = new DataForm

  lazy val cached = new Cached(
    createdTtl = CreatedCacheTtl,
    rankingTtl = RankingCacheTtl)

  lazy val api = new TournamentApi(
    cached = cached,
    scheduleJsonView = scheduleJsonView,
    system = system,
    sequencers = sequencerMap,
    autoPairing = autoPairing,
    clearJsonViewCache = jsonView.clearCache,
    router = hub.actor.router,
    renderer = hub.actor.renderer,
    timeline = hub.actor.timeline,
    socketHub = socketHub,
    site = hub.socket.site,
    lobby = hub.socket.lobby,
    trophyApi = trophyApi,
    indexLeaderboard = leaderboardIndexer.indexOne _,
    roundMap = roundMap,
    roundSocketHub = roundSocketHub)

  val tourAndRanks = api tourAndRanks _

  private lazy val performance = new Performance

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood)

  lazy val winners = new Winners(
    mongoCache = mongoCache,
    ttl = LeaderboardCacheTtl)

  lazy val jsonView = new JsonView(lightUser, cached, performance)

  lazy val scheduleJsonView = new ScheduleJsonView(lightUser)

  lazy val leaderboardApi = new LeaderboardApi(
    coll = leaderboardColl,
    maxPerPage = 20)

  private lazy val leaderboardIndexer = new LeaderboardIndexer(
    tournamentColl = tournamentColl,
    leaderboardColl = leaderboardColl)

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(tournamentId: String) = new Socket(
        tournamentId = tournamentId,
        history = new History(ttl = HistoryMessageTtl),
        jsonView = jsonView,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightUser = lightUser)
    }), name = SocketName)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(
      receiveTimeout = SequencerTimeout.some,
      executionTimeout = 5.seconds.some)
  }))

  private val organizer = system.actorOf(Props(new Organizer(
    api = api,
    reminder = system.actorOf(Props(new Reminder(
      renderer = hub.actor.renderer
    )), name = ReminderName),
    isOnline = isOnline,
    socketHub = socketHub
  )), name = OrganizerName)

  private val tournamentScheduler = system.actorOf(Props(new Scheduler(api)))

  def version(tourId: String): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  def cli = new lila.common.Cli {
    def process = {
      case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        leaderboardIndexer.generateAll inject "Done!"
    }
  }

  private lazy val autoPairing = new AutoPairing(
    roundMap = roundMap,
    system = system,
    onStart = onStart)

  {
    import scala.concurrent.duration._

    scheduler.message(2 seconds) {
      organizer -> actorApi.AllCreatedTournaments
    }

    scheduler.message(3 seconds) {
      organizer -> actorApi.StartedTournaments
    }

    scheduler.message(5 minutes) {
      tournamentScheduler -> actorApi.ScheduleNow
    }
  }

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
  private[tournament] lazy val pairingColl = db(CollectionPairing)
  private[tournament] lazy val playerColl = db(CollectionPlayer)
  private[tournament] lazy val leaderboardColl = db(CollectionLeaderboard)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "tournament" boot new Env(
    config = lila.common.PlayApp loadConfig "tournament",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMap = lila.round.Env.current.roundMap,
    roundSocketHub = lila.hub.Env.current.socket.round,
    lightUser = lila.user.Env.current.lightUser,
    isOnline = lila.user.Env.current.isOnline,
    onStart = lila.game.Env.current.onStart,
    trophyApi = lila.user.Env.current.trophyApi,
    scheduler = lila.common.PlayApp.scheduler)
}
