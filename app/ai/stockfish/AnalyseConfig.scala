package lila
package ai.stockfish

import model._
import model.analyse._
import core.Settings

final class AnalyseConfig(settings: Settings) extends Config {

  type Instructions = List[String]

  def init: Instructions = List(
    setoption("Uci_AnalyseMode", true),
    setoption("Hash", settings.AiStockfishAnalyseHashSize),
    setoption("Threads", 8),
    setoption("Ponder", false)
  )

  def game(analyse: Analyse): Instructions = List(
    setoption("UCI_Chess960", analyse.chess960)
  )

  def moveTime = settings.AiStockfishAnalyseMoveTime
}
