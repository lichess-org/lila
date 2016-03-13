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
    saveAnalysis: lila.analyse.Analysis => Funit) {

  private val ActorName = config getString "actor.name"
  private val OfflineMode = config getBoolean "offline_mode"

  private val moveColl = db(config getString "collection.move")
  private val analysisColl = db(config getString "collection.analysis")
  private val clientColl = db(config getString "collection.client")

  private val repo = new FishnetRepo(
    moveColl = moveColl,
    analysisColl = analysisColl,
    clientColl = clientColl)

  private val sequencer = new Sequencer(
    move = new lila.hub.FutureSequencer(
      system = system,
      receiveTimeout = None,
      executionTimeout = Some(200 millis)),
    analysis = new lila.hub.FutureSequencer(
      system = system,
      receiveTimeout = None,
      executionTimeout = Some(500 millis)))

  private val monitor = new Monitor(repo, scheduler) // start monitoring

  val api = new FishnetApi(
    hub = hub,
    repo = repo,
    moveColl = moveColl,
    analysisColl = analysisColl,
    clientColl = clientColl,
    sequencer = sequencer,
    monitor = monitor,
    saveAnalysis = saveAnalysis,
    offlineMode = OfflineMode)

  val player = new Player(
    repo = repo,
    uciMemo = uciMemo,
    sequencer = sequencer)

  val analyser = new Analyser(
    repo = repo,
    uciMemo = uciMemo,
    sequencer = sequencer,
    limiter = new Limiter(analysisColl))

  val aiPerfApi = new AiPerfApi

  private val cleaner = new Cleaner(
    repo = repo,
    moveColl = moveColl,
    analysisColl = analysisColl,
    monitor = monitor,
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
      case "fishnet" :: "client" :: "create" :: key :: userId :: skill :: Nil =>
        api.createClient(Client.Key(key), Client.UserId(userId), skill) inject "done!"
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
    saveAnalysis = lila.analyse.Env.current.analyser.save _)
}
