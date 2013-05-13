package lila.ai

import lila.common.PimpedConfig._

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val EngineName = config getString "engine"
    val IsServer = config getBoolean "server"
    val IsClient = config getBoolean "client"
    val StockfishExecPath = config getString "stockfish.exec_path"
    val StockfishPlayUrl = config getString "stockfish.play.url"
    val StockfishAnalyseUrl = config getString "stockfish.analyse.url"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val ai: Ai = (EngineName, IsClient) match {
    case ("stockfish", true)  ⇒ stockfishClient or stockfishAi
    case ("stockfish", false) ⇒ stockfishAi
    case _                    ⇒ stupidAi
  }

  def clientDiagnose { client foreach (_.diagnose) }

  def clientPing = client flatMap (_.currentPing)

  def isServer = IsServer

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.Ping ⇒ sender ! clientPing
      case lila.hub.actorApi.ai.Analyse(id, pgn, fen) ⇒
        ai.analyse(pgn, fen) map { _(id) } pipeTo sender
    }
  }), name = ActorName)

  {
    import scala.concurrent.duration._

    scheduler.effect(10 seconds, "ai: diagnose") {
      clientDiagnose
    }

    scheduler.once(10 millis) { clientDiagnose }
  }

  private lazy val stockfishAi = new stockfish.Ai(
    server = stockfishServer)

  private lazy val stockfishClient = new stockfish.Client(
    playUrl = StockfishPlayUrl,
    analyseUrl = StockfishAnalyseUrl)

  lazy val stockfishServer = new stockfish.Server(
    execPath = StockfishExecPath,
    config = stockfishConfig)

  private lazy val stupidAi = new StupidAi

  private lazy val stockfishConfig = new stockfish.Config(
    hashSize = config getInt "stockfish.hash_size",
    nbThreads = config getInt "stockfish.threads",
    playMaxMoveTime = config getInt "stockfish.play.movetime",
    analyseMoveTime = config getInt "stockfish.analyse.movetime",
    debug = config getBoolean "stockfish.debug")

  private lazy val client = (EngineName, IsClient) match {
    case ("stockfish", true) ⇒ stockfishClient.some
    case _                   ⇒ none
  }

  private lazy val server = (EngineName, IsServer) match {
    case ("stockfish", true) ⇒ stockfishServer.some
    case _                   ⇒ none
  }
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    config = lila.common.PlayApp loadConfig "ai",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
