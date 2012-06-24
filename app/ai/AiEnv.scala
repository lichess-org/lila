package lila
package ai

import com.mongodb.casbah.MongoCollection

import core.Settings

final class AiEnv(settings: Settings) {

  import settings._

  val ai: () ⇒ Ai = (AiChoice, AiClientMode) match {
    case (AiStockfish, false) ⇒ () ⇒ stockfishAi
    case (AiStockfish, true)  ⇒ () ⇒ stockfishClient or stockfishAi
    case (AiCrafty, false)    ⇒ () ⇒ craftyAi
    case (AiCrafty, true)     ⇒ () ⇒ craftyClient or craftyAi
    case _                    ⇒ () ⇒ stupidAi
  }

  lazy val client: Client = AiChoice match {
    case AiStockfish ⇒ stockfishClient
    case AiCrafty    ⇒ craftyClient
  }

  lazy val craftyAi = new crafty.Ai(
    server = craftyServer)

  lazy val craftyClient = new crafty.Client(
    playUrl = AiCraftyPlayUrl)

  lazy val craftyServer = new crafty.Server(
    execPath = AiCraftyExecPath,
    bookPath = AiCraftyBookPath)

  lazy val stockfishAi = new stockfish.Ai(
    server = stockfishServer)

  lazy val stockfishClient = new stockfish.Client(
    playUrl = AiStockfishPlayUrl,
    analyseUrl = AiStockfishAnalyseUrl)

  lazy val stockfishServer = new stockfish.Server(
    execPath = AiStockfishExecPath,
    playConfig = stockfishPlayConfig,
    analyseConfig = stockfishAnalyseConfig)

  lazy val stockfishPlayConfig = new stockfish.PlayConfig(settings)
  lazy val stockfishAnalyseConfig = new stockfish.AnalyseConfig(settings)

  lazy val stupidAi = new StupidAi

  val isServer = AiServerMode
}
