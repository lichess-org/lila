package lila.fishnet

import akka.actor._
import com.typesafe.config.Config
import io.lettuce.core._
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.common.Bus

final class Env(
    config: Config,
    uciMemo: lila.game.UciMemo,
    requesterApi: lila.analyse.RequesterApi,
    evalCacheApi: lila.evalCache.EvalCacheApi,
    hub: lila.hub.Env,
    db: lila.db.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    asyncCache: lila.memo.AsyncCache.Builder,
    sink: lila.analyse.Analyser
) {

  private val ActorName = config getString "actor.name"
  private val OfflineMode = config getBoolean "offline_mode"
  private val AnalysisNodes = config getInt "analysis.nodes"
  private val MovePlies = config getInt "move.plies"
  private val ClientMinVersion = config getString "client_min_version"
  private val RedisUri = config getString "redis.uri"

  private val analysisColl = db(config getString "collection.analysis")
  private val clientColl = db(config getString "collection.client")

  private val redis = new FishnetRedis(
    RedisClient create RedisURI.create(RedisUri),
    "fishnet-in",
    "fishnet-out",
    system
  )

  private val clientVersion = new Client.ClientVersion(ClientMinVersion)

  private val repo = new FishnetRepo(
    analysisColl = analysisColl,
    clientColl = clientColl,
    asyncCache = asyncCache
  )

  private val sequencer = new lila.hub.FutureSequencer(
    system = system,
    executionTimeout = Some(1 second),
    logger = logger
  )

  private val monitor = new Monitor(repo, sequencer, scheduler)

  private val evalCache = new FishnetEvalCache(evalCacheApi)

  private val analysisBuilder = new AnalysisBuilder(evalCache)

  val api = new FishnetApi(
    repo = repo,
    analysisBuilder = analysisBuilder,
    analysisColl = analysisColl,
    sequencer = sequencer,
    monitor = monitor,
    sink = sink,
    socketExists = id => Bus.ask[Boolean]('roundSocket)(lila.hub.actorApi.map.Exists(id, _))(system),
    clientVersion = clientVersion,
    offlineMode = OfflineMode,
    analysisNodes = AnalysisNodes
  )(system)

  val player = new Player(
    redis = redis,
    uciMemo = uciMemo,
    maxPlies = MovePlies
  )(system)

  private val limiter = new Limiter(
    analysisColl = analysisColl,
    requesterApi = requesterApi
  )

  val analyser = new Analyser(
    repo = repo,
    uciMemo = uciMemo,
    sequencer = sequencer,
    evalCache = evalCache,
    limiter = limiter
  )

  val aiPerfApi = new AiPerfApi

  new Cleaner(
    repo = repo,
    analysisColl = analysisColl,
    monitor = monitor,
    scheduler = scheduler
  )

  new MainWatcher(repo = repo, scheduler = scheduler)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.fishnet.AutoAnalyse(gameId) =>
        analyser(gameId, Work.Sender(userId = none, ip = none, mod = false, system = true))
      case req: lila.hub.actorApi.fishnet.StudyChapterRequest => analyser study req
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    def process = {
      case "fishnet" :: "client" :: "create" :: userId :: skill :: Nil =>
        api.createClient(Client.UserId(userId.toLowerCase), skill) map { client =>
          Bus.publish(lila.hub.actorApi.fishnet.NewKey(userId, client.key.value), 'fishnet)
          s"Created key: ${(client.key.value)} for: $userId"
        }
      case "fishnet" :: "client" :: "delete" :: key :: Nil =>
        repo toKey key flatMap repo.deleteClient inject "done!"
      case "fishnet" :: "client" :: "enable" :: key :: Nil =>
        repo toKey key flatMap { repo.enableClient(_, true) } inject "done!"
      case "fishnet" :: "client" :: "disable" :: key :: Nil =>
        repo toKey key flatMap { repo.enableClient(_, false) } inject "done!"
      case "fishnet" :: "client" :: "skill" :: key :: skill :: Nil =>
        repo toKey key flatMap { api.setClientSkill(_, skill) } inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "fishnet" boot new Env(
    system = lila.common.PlayApp.system,
    uciMemo = lila.game.Env.current.uciMemo,
    requesterApi = lila.analyse.Env.current.requesterApi,
    evalCacheApi = lila.evalCache.Env.current.api,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet",
    scheduler = lila.common.PlayApp.scheduler,
    asyncCache = lila.memo.Env.current.asyncCache,
    sink = lila.analyse.Env.current.analyser
  )
}
