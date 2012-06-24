package lila
package ai.stockfish

import model._
import model.play._
import core.Settings

final class PlayConfig(settings: Settings) extends Config {

  type Instructions = List[String]

  def init: Instructions = List(
    setoption("Hash", settings.AiStockfishPlayHashSize),
    setoption("Threads", 8),
    setoption("Ponder", false),
    setoption("Aggressiveness", settings.AiStockfishPlayAggressiveness) // 0 - 200
  )

  def game(play: Play): Instructions = List(
    setoption("Skill Level", skill(play.level)),
    setoption("UCI_Chess960", play.chess960)
  )

  def maxMoveTime = settings.AiStockfishPlayMaxMoveTime

  def moveTime(level: Int): Int = 
    maxMoveTime / (level >= 1 && level <= 8).fold(9 - level, 8)

  private def skill(level: Int) = 
    (level >= 1 && level <= 8).fold(math.round((level -1) * (20 / 7f)), 0)
}
