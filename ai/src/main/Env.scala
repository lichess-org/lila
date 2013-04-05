package lila.ai

import lila.common.PimpedConfig._

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(config: Config) {

  private val settings = new {

    val EngineName = config getString "engine" 
    val ServerMode = config getBoolean "server"
    val ClientMode = config getBoolean "client"

    val StockfishExecPath = config getString "stockfish.exec_path"
    val StockfishHashSize = config getInt "stockfish.hash_size"
    val StockfishThreads = config getInt "stockfish.threads"
    val StockfishPlayUrl = config getString "stockfish.play.url"
    val StockfishPlayMaxMoveTime = config getInt "stockfish.play.movetime"
    val StockfishAnalyseUrl = config getString "stockfish.analyse.url"
    val StockfishAnalyseMoveTime = config getInt "stockfish.analyse.movetime"
    val StockfishDebug = config getBoolean "stockfish.debug"
  }
  import settings._
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    config = lila.common.PlayApp loadConfig "ai")
}
