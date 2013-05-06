package lila.ai

import lila.common.PimpedConfig._

import akka.actor.{ ActorRef, ActorSystem }
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
  }
  import settings._

  lazy val ai: Ai = (EngineName, IsClient) match {
    case ("stockfish", true)  ⇒ stockfishClient or stockfishAi
    case ("stockfish", false) ⇒ stockfishAi
    case _                    ⇒ stupidAi
  }

  def clientDiagnose { client foreach (_.diagnose) }

  def clientPing = client flatMap (_.currentPing)

  def stockfishServerReport = (IsServer && EngineName == "stockfish") option {
    stockfishServer.report
  }

  def isServer = IsServer

  {
    import scala.concurrent.duration._

    scheduler.effect(10 seconds, "ai: diagnose") {
      clientDiagnose
    }

    scheduler.once(5 millis) { clientDiagnose }
  }

  private lazy val stockfishAi = new stockfish.Ai(
    server = stockfishServer)

  private lazy val stockfishClient = new stockfish.Client(
    playUrl = StockfishPlayUrl,
    analyseUrl = StockfishAnalyseUrl)

  private lazy val stockfishServer = new stockfish.Server(
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
