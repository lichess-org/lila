package lila.ai

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem) {

  private val settings = new {
    val EngineName = config getString "engine"
    val IsServer = config getBoolean "server"
    val IsClient = config getBoolean "client"
    val StockfishPlayUrl = config getString "stockfish.play.url"
    val StockfishAnalyseUrl = config getString "stockfish.analyse.url"
    val StockfishLoadUrl = config getString "stockfish.load.url"
    val StockfishQueueName = config getString "stockfish.queue.name"
    val StockfishQueueDispatcher = config getString "stockfish.queue.dispatcher"
    val StockfishAnalyseTimeout = config duration "stockfish.analyse.timeout"
    val ActorName = config getString "actor.name"
  }
  import settings._

  private val stockfishConfig = new stockfish.Config(
    execPath = config getString "stockfish.exec_path",
    hashSize = config getInt "stockfish.hash_size",
    nbThreads = config getInt "stockfish.threads",
    playMaxMoveTime = config duration "stockfish.play.movetime",
    analyseMoveTime = config duration "stockfish.analyse.movetime",
    playTimeout = config duration "stockfish.play.timeout",
    analyseTimeout = config duration "stockfish.analyse.timeout",
    debug = config getBoolean "stockfish.debug")

  def ai: () ⇒ Fu[Ai] = () ⇒ (EngineName, IsClient) match {
    case ("stockfish", true)  ⇒ stockfishClient or stockfishAi
    case ("stockfish", false) ⇒ fuccess(stockfishAi)
    case _                    ⇒ fuccess(stupidAi)
  }

  def isServer = IsServer

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.GetLoad ⇒ IsClient.fold(
        stockfishClient.load pipeTo sender,
        sender ! none
      )
      case lila.hub.actorApi.ai.Analyse(id, pgn, fen) ⇒
        ai() flatMap { _.analyse(pgn, fen) } map { _(id) } pipeTo sender
    }
  }), name = ActorName)

  private lazy val stockfishAi = new stockfish.Ai(stockfishServer)

  private lazy val stockfishClient = new stockfish.Client(
    playUrl = StockfishPlayUrl,
    analyseUrl = StockfishAnalyseUrl,
    loadUrl = StockfishLoadUrl,
    requestTimeout = StockfishAnalyseTimeout,
    system = system)

  lazy val stockfishServer = new stockfish.Server(
    queue = stockfishQueue,
    config = stockfishConfig)

  private lazy val stockfishQueue = system.actorOf(Props(
    new stockfish.Queue(stockfishConfig, system)
  ) withDispatcher StockfishQueueDispatcher, name = StockfishQueueName)

  private lazy val stupidAi = new StupidAi

  private lazy val client = (EngineName, IsClient) match {
    case ("stockfish", true) ⇒ stockfishClient.some
    case _                   ⇒ none
  }
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    config = lila.common.PlayApp loadConfig "ai",
    system = lila.common.PlayApp.system)
}
