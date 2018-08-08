package lidraughts.tournament

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.hub.actorApi.map.Ask
import lidraughts.hub.{ ActorMap, Sequencer }
import lidraughts.socket.actorApi.GetVersion
import lidraughts.socket.History
import lidraughts.user.User
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lidraughts.db.Env,
    mongoCache: lidraughts.memo.MongoCache.Builder,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    flood: lidraughts.security.Flood,
    hub: lidraughts.hub.Env,
    roundMap: ActorRef,
    roundSocketHub: ActorSelection,
    lightUserApi: lidraughts.user.LightUserApi,
    isOnline: String => Boolean,
    onStart: String => Unit,
    historyApi: lidraughts.history.HistoryApi,
    trophyApi: lidraughts.user.TrophyApi,
    notifyApi: lidraughts.notify.NotifyApi,
    scheduler: lidraughts.common.Scheduler,
    startedSinceSeconds: Int => Boolean
) {

  private val startsAtMillis = nowMillis

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
    val ApiActorName = config getString "api_actor.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val ChannelStanding = config getString "channel.standing.name "
    val NetDomain = config getString "net.domain"
  }
  import settings._

  lazy val forms = new DataForm

  lazy val cached = new Cached(
    asyncCache = asyncCache,
    createdTtl = CreatedCacheTtl,
    rankingTtl = RankingCacheTtl
  )(system)

  lazy val verify = new Condition.Verify(historyApi)

  lazy val winners = new WinnersApi(
    coll = tournamentColl,
    mongoCache = mongoCache,
    ttl = LeaderboardCacheTtl,
    scheduler = scheduler
  )

  lazy val statsApi = new TournamentStatsApi(
    mongoCache = mongoCache
  )

  lazy val shieldApi = new TournamentShieldApi(
    coll = tournamentColl,
    asyncCache = asyncCache
  )

  lazy val revolutionApi = new RevolutionApi(
    coll = tournamentColl,
    asyncCache = asyncCache
  )

  private val duelStore = new DuelStore

  lazy val api = new TournamentApi(
    cached = cached,
    scheduleJsonView = scheduleJsonView,
    system = system,
    sequencers = sequencerMap,
    autoPairing = autoPairing,
    clearJsonViewCache = jsonView.clearCache,
    clearWinnersCache = winners.clearCache,
    clearTrophyCache = tour => {
      if (tour.isShield) shieldApi.clear
      else if (tour.isUnique) revolutionApi.clear
    },
    renderer = hub.actor.renderer,
    timeline = hub.actor.timeline,
    socketHub = socketHub,
    site = hub.socket.site,
    lobby = hub.socket.lobby,
    trophyApi = trophyApi,
    verify = verify,
    indexLeaderboard = leaderboardIndexer.indexOne _,
    roundMap = roundMap,
    asyncCache = asyncCache,
    duelStore = duelStore,
    lightUserApi = lightUserApi
  )

  lazy val crudApi = new crud.CrudApi

  val tourAndRanks = api tourAndRanks _

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood
  )

  lazy val jsonView = new JsonView(lightUserApi, cached, statsApi, shieldApi, asyncCache, verify, duelStore, startedSinceSeconds)

  lazy val scheduleJsonView = new ScheduleJsonView(lightUserApi.async)

  lazy val leaderboardApi = new LeaderboardApi(
    coll = leaderboardColl,
    maxPerPage = lidraughts.common.MaxPerPage(15)
  )

  def playerRepo = PlayerRepo

  private lazy val leaderboardIndexer = new LeaderboardIndexer(
    tournamentColl = tournamentColl,
    leaderboardColl = leaderboardColl
  )

  private val socketHub = system.actorOf(
    Props(new lidraughts.socket.SocketHubActor.Default[Socket] {
      def mkActor(tournamentId: String) = new Socket(
        tournamentId = tournamentId,
        history = new History(ttl = HistoryMessageTtl),
        jsonView = jsonView,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightUser = lightUserApi.async
      )
    }), name = SocketName
  )

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(
      receiveTimeout = SequencerTimeout.some,
      executionTimeout = 5.seconds.some,
      logger = logger
    )
  }))

  system.lidraughtsBus.subscribe(
    system.actorOf(Props(new ApiActor(api = api)), name = ApiActorName),
    'finishGame, 'adjustCheater, 'adjustBooster, 'playban
  )

  system.actorOf(Props(new CreatedOrganizer(
    api = api,
    isOnline = isOnline
  )))

  private val reminder = system.actorOf(Props[Reminder])

  system.actorOf(Props(new StartedOrganizer(
    api = api,
    reminder = reminder,
    isOnline = isOnline,
    socketHub = socketHub
  )))

  TournamentScheduler.start(system, api)

  TournamentInviter.start(system, api, notifyApi)

  def version(tourId: Tournament.ID): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  // is that user playing a game of this tournament
  // or hanging out in the tournament lobby (joined or not)
  def hasUser(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] = {
    socketHub ? Ask(tourId, lidraughts.hub.actorApi.HasUserId(userId)) mapTo manifest[Boolean]
  } >>| PairingRepo.isPlaying(tourId, userId)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "tournament" :: "leaderboard" :: "generate" :: Nil =>
        leaderboardIndexer.generateAll inject "Done!"
    }
  }

  private lazy val autoPairing = new AutoPairing(duelStore, onStart)

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
  private[tournament] lazy val pairingColl = db(CollectionPairing)
  private[tournament] lazy val playerColl = db(CollectionPlayer)
  private[tournament] lazy val leaderboardColl = db(CollectionLeaderboard)
}

object Env {

  lazy val current = "tournament" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "tournament",
    system = lidraughts.common.PlayApp.system,
    db = lidraughts.db.Env.current,
    mongoCache = lidraughts.memo.Env.current.mongoCache,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    flood = lidraughts.security.Env.current.flood,
    hub = lidraughts.hub.Env.current,
    roundMap = lidraughts.round.Env.current.roundMap,
    roundSocketHub = lidraughts.hub.Env.current.socket.round,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    isOnline = lidraughts.user.Env.current.isOnline,
    onStart = lidraughts.game.Env.current.onStart,
    historyApi = lidraughts.history.Env.current.api,
    trophyApi = lidraughts.user.Env.current.trophyApi,
    notifyApi = lidraughts.notify.Env.current.api,
    scheduler = lidraughts.common.PlayApp.scheduler,
    startedSinceSeconds = lidraughts.common.PlayApp.startedSinceSeconds
  )
}
