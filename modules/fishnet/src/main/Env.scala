package lila.fishnet

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    uciMemo: lila.game.UciMemo,
    requesterApi: lila.analyse.RequesterApi,
    hub: lila.hub.Env,
    db: lila.db.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    bus: lila.common.Bus,
    sink: lila.analyse.Analyser) {

  private val ActorName = config getString "actor.name"
  private val OfflineMode = config getBoolean "offline_mode"
  private val AnalysisNodes = config getInt "analysis.nodes"
  private val MovePlies = config getInt "move.plies"

  private val analysisColl = db(config getString "collection.analysis")
  private val requesterColl = db(config getString "collection.requester")
  private val clientColl = db(config getString "collection.client")

  private val repo = new FishnetRepo(
    analysisColl = analysisColl,
    clientColl = clientColl)

  private val moveDb = new MoveDB(
    roundMap = hub.actor.roundMap,
    system = system)

  private val sequencer = new lila.hub.FutureSequencer(
    system = system,
    receiveTimeout = None,
    executionTimeout = Some(1 second),
    logger = logger)

  private val monitor = new Monitor(moveDb, repo, sequencer, scheduler)

  val api = new FishnetApi(
    repo = repo,
    moveDb = moveDb,
    analysisColl = analysisColl,
    sequencer = sequencer,
    monitor = monitor,
    sink = sink,
    socketExists = id => {
    import lila.hub.actorApi.map.Exists
    import akka.pattern.ask
    import makeTimeout.short
    hub.socket.round ? Exists(id) mapTo manifest[Boolean]
  },
    offlineMode = OfflineMode,
    analysisNodes = AnalysisNodes)(system)

  val player = new Player(
    moveDb = moveDb,
    uciMemo = uciMemo,
    maxPlies = MovePlies)

  private val limiter = new Limiter(
    analysisColl = analysisColl,
    requesterApi = requesterApi)

  val analyser = new Analyser(
    repo = repo,
    uciMemo = uciMemo,
    sequencer = sequencer,
    limiter = limiter)

  val aiPerfApi = new AiPerfApi

  new Cleaner(
    repo = repo,
    moveDb = moveDb,
    analysisColl = analysisColl,
    monitor = monitor,
    scheduler = scheduler)

  new MainWatcher(
    repo = repo,
    bus = bus,
    scheduler = scheduler)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.fishnet.AutoAnalyse(gameId) =>
        analyser(gameId, Work.Sender(userId = none, ip = none, mod = false, system = true))
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    def process = {
      case "fishnet" :: "client" :: "create" :: userId :: skill :: Nil =>
        api.createClient(Client.UserId(userId), skill) map (_.key.value)
      case "fishnet" :: "client" :: "delete" :: key :: Nil =>
        repo.deleteClient(Client.Key(key)) inject "done!"
      case "fishnet" :: "client" :: "enable" :: key :: Nil =>
        repo.enableClient(Client.Key(key), true) inject "done!"
      case "fishnet" :: "client" :: "disable" :: key :: Nil =>
        repo.enableClient(Client.Key(key), false) inject "done!"
      case "fishnet" :: "client" :: "skill" :: key :: skill :: Nil =>
        api.setClientSkill(Client.Key(key), skill) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "fishnet" boot new Env(
    system = lila.common.PlayApp.system,
    uciMemo = lila.game.Env.current.uciMemo,
    requesterApi = lila.analyse.Env.current.requesterApi,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet",
    scheduler = lila.common.PlayApp.scheduler,
    bus = lila.common.PlayApp.system.lilaBus,
    sink = lila.analyse.Env.current.analyser)
}
