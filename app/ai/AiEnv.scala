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
    remoteUrl = AiCraftyRemoteUrl)

  lazy val craftyServer = new crafty.Server(
    execPath = AiCraftyExecPath,
    bookPath = AiCraftyBookPath)

  lazy val stockfishAi = new stockfish.Ai(
    server = stockfishServer)

  lazy val stockfishClient = new stockfish.Client(
    remoteUrl = AiStockfishRemoteUrl)

  lazy val stockfishServer = new stockfish.Server(
    execPath = AiStockfishExecPath,
    config = stockfishConfig)

  lazy val stockfishConfig = new stockfish.Config(settings)

  lazy val stupidAi = new StupidAi

  val isServer = AiServerMode
}
