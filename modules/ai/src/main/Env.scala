package lila.ai

import scala.collection.JavaConversions._

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    uciMemo: lila.game.UciMemo,
    system: ActorSystem) {

  private val settings = new {
    val EngineName = config getString "engine"
    val IsServer = config getBoolean "server"
    val IsClient = config getBoolean "client"
    val StockfishRemotes = config getStringList "stockfish.remotes" toList
    val StockfishLocal = config getString "stockfish.local"
    val StockfishPlayRoute = config getString "stockfish.play.route"
    val StockfishAnalyseRoute = config getString "stockfish.analyse.route"
    val StockfishLoadRoute = config getString "stockfish.load.route"
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
    loadTimeout = config duration "stockfish.load.timeout",
    analyseTimeout = config duration "stockfish.analyse.timeout",
    debug = config getBoolean "stockfish.debug")

  lazy val ai: Ai = (EngineName, IsClient) match {
    case ("stockfish", true)  ⇒ stockfishClient
    case ("stockfish", false) ⇒ stockfishServer
    case _                    ⇒ throw new Exception(s"Unsupported AI: $EngineName")
  }

  def isServer = IsServer

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.GetLoad ⇒ IsClient.fold(
        stockfishClient.load pipeTo sender,
        sender ! Nil
      )
      case lila.hub.actorApi.ai.Analyse(uciMoves, fen) ⇒
        ai.analyse(uciMoves, fen) pipeTo sender
    }
  }), name = ActorName)

  lazy val stockfishClient = new stockfish.Client(
    dispatcher = system.actorOf(
      Props(new stockfish.remote.Dispatcher(
        urls = StockfishRemotes,
        config = stockfishConfig,
        router = stockfish.remote.Router(
          playRoute = StockfishPlayRoute,
          analyseRoute = StockfishAnalyseRoute,
          loadRoute = StockfishLoadRoute) _
      )
      ), name = "stockfish-dispatcher"),
    fallback = stockfishServer,
    config = stockfishConfig,
    uciMemo = uciMemo)

  lazy val stockfishServer = new stockfish.Server(
    queue = stockfishQueue,
    config = stockfishConfig,
    host = DNSLookup(StockfishLocal),
    uciMemo = uciMemo)

  def nbStockfishRemotes = StockfishRemotes.size

  private lazy val stockfishQueue = system.actorOf(Props(
    new stockfish.Queue(stockfishConfig)
  ) withDispatcher StockfishQueueDispatcher, name = StockfishQueueName)

  private lazy val client = (EngineName, IsClient) match {
    case ("stockfish", true) ⇒ stockfishClient.some
    case _                   ⇒ none
  }
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    config = lila.common.PlayApp loadConfig "ai",
    uciMemo = lila.game.Env.current.uciMemo,
    system = lila.common.PlayApp.system)
}
