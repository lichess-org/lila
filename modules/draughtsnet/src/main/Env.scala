package lidraughts.draughtsnet

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.Promise

final class Env(
    config: Config,
    uciMemo: lidraughts.game.UciMemo,
    requesterApi: lidraughts.analyse.RequesterApi,
    evalCacheApi: lidraughts.evalCache.EvalCacheApi,
    hub: lidraughts.hub.Env,
    db: lidraughts.db.Env,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    bus: lidraughts.common.Bus,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    sink: lidraughts.analyse.Analyser
) {

  private val ActorName = config getString "actor.name"
  private val OfflineMode = config getBoolean "offline_mode"
  private val AnalysisNodes = config getInt "analysis.nodes"
  private val CommentaryNodes = config getInt "commentary.nodes"
  private val MovePlies = config getInt "move.plies"
  private val ClientMinVersion = config getString "client_min_version"

  private val evalCacheMinNodes = (AnalysisNodes * 0.9).intValue

  private val analysisColl = db(config getString "collection.analysis")
  private val clientColl = db(config getString "collection.client")

  private val clientVersion = new ClientVersion(ClientMinVersion)

  private val repo = new DraughtsnetRepo(
    analysisColl = analysisColl,
    clientColl = clientColl,
    asyncCache = asyncCache
  )

  private val moveDb = new MoveDB(system = system)

  private val sequencer = new lidraughts.hub.FutureSequencer(
    system = system,
    executionTimeout = Some(1 second),
    logger = logger
  )

  private val monitor = new Monitor(moveDb, repo, sequencer, scheduler)

  private val evalCache = new DraughtsnetEvalCache(evalCacheApi)

  private val analysisBuilder = new AnalysisBuilder(evalCache, evalCacheMinNodes)

  private val commentDb = new CommentDB(
    evalCache = evalCache,
    bus = bus,
    system = system
  )

  val api = new DraughtsnetApi(
    repo = repo,
    moveDb = moveDb,
    commentDb = commentDb,
    analysisBuilder = analysisBuilder,
    analysisColl = analysisColl,
    sequencer = sequencer,
    monitor = monitor,
    sink = sink,
    socketExists = id => bus.ask[Boolean]('roundSocket)(lidraughts.hub.actorApi.map.Exists(id, _)),
    clientVersion = clientVersion,
    offlineMode = OfflineMode,
    analysisNodes = AnalysisNodes,
    commentaryNodes = CommentaryNodes
  )(system)

  val player = new Player(
    moveDb = moveDb,
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
    analysisBuilder = analysisBuilder,
    sequencer = sequencer,
    evalCache = evalCache,
    limiter = limiter,
    evalCacheMinNodes = evalCacheMinNodes
  )

  val commentator = new Commentator(
    bus = bus,
    commentDb = commentDb,
    uciMemo = uciMemo,
    evalCacheApi = evalCacheApi,
    maxPlies = MovePlies
  )

  val aiPerfApi = new AiPerfApi

  new Cleaner(
    repo = repo,
    moveDb = moveDb,
    commentDb = commentDb,
    analysisColl = analysisColl,
    monitor = monitor,
    scheduler = scheduler
  )

  new MainWatcher(
    repo = repo,
    bus = bus,
    scheduler = scheduler
  )

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.hub.actorApi.draughtsnet.AutoAnalyse(gameId) =>
        analyser(gameId, Work.Sender(userId = none, ip = none, mod = false, system = true))
      case req: lidraughts.hub.actorApi.draughtsnet.StudyChapterRequest => analyser study req
    }
  }), name = ActorName)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "draughtsnet" :: "client" :: "create" :: userId :: skill :: Nil =>
        api.createClient(Client.UserId(userId.toLowerCase), skill) map { client =>
          bus.publish(lidraughts.hub.actorApi.draughtsnet.NewKey(userId, client.key.value), 'draughtsnet)
          s"Created key: ${(client.key.value)} for: $userId"
        }
      case "draughtsnet" :: "client" :: "delete" :: key :: Nil =>
        repo toKey key flatMap repo.deleteClient inject "done!"
      case "draughtsnet" :: "client" :: "enable" :: key :: Nil =>
        repo toKey key flatMap { repo.enableClient(_, true) } inject "done!"
      case "draughtsnet" :: "client" :: "disable" :: key :: Nil =>
        repo toKey key flatMap { repo.enableClient(_, false) } inject "done!"
      case "draughtsnet" :: "client" :: "skill" :: key :: skill :: Nil =>
        repo toKey key flatMap { api.setClientSkill(_, skill) } inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "draughtsnet" boot new Env(
    system = lidraughts.common.PlayApp.system,
    uciMemo = lidraughts.game.Env.current.uciMemo,
    requesterApi = lidraughts.analyse.Env.current.requesterApi,
    evalCacheApi = lidraughts.evalCache.Env.current.api,
    hub = lidraughts.hub.Env.current,
    db = lidraughts.db.Env.current,
    config = lidraughts.common.PlayApp loadConfig "draughtsnet",
    scheduler = lidraughts.common.PlayApp.scheduler,
    bus = lidraughts.common.PlayApp.system.lidraughtsBus,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    sink = lidraughts.analyse.Env.current.analyser
  )
}
