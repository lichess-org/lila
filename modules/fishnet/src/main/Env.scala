package lila.fishnet

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    uciMemo: lila.game.UciMemo,
    hub: lila.hub.Env,
    db: lila.db.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    bus: lila.common.Bus,
    saveAnalysis: lila.analyse.Analysis => Funit) {

  private val ActorName = config getString "actor.name"
  private val OfflineMode = config getBoolean "offline_mode"

  private val analysisColl = db(config getString "collection.analysis")
  private val clientColl = db(config getString "collection.client")

  private val repo = new FishnetRepo(
    analysisColl = analysisColl,
    clientColl = clientColl)

  private val moveDb = new MoveDB

  private val sequencer = new lila.hub.FutureSequencer(
    system = system,
    receiveTimeout = None,
    executionTimeout = Some(500 millis))

  private val monitor = new Monitor(moveDb, repo, sequencer, scheduler)

  val api = new FishnetApi(
    hub = hub,
    repo = repo,
    moveDb = moveDb,
    analysisColl = analysisColl,
    clientColl = clientColl,
    sequencer = sequencer,
    monitor = monitor,
    saveAnalysis = saveAnalysis,
    offlineMode = OfflineMode)

  val player = new Player(
    moveDb = moveDb,
    uciMemo = uciMemo)

  val analyser = new Analyser(
    repo = repo,
    uciMemo = uciMemo,
    sequencer = sequencer,
    limiter = new Limiter(analysisColl))

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
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet",
    scheduler = lila.common.PlayApp.scheduler,
    bus = lila.common.PlayApp.system.lilaBus,
    saveAnalysis = lila.analyse.Env.current.analyser.save _)
}
