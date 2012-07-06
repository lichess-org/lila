package lila
package ai.stockfish

import model._
import core.Settings

final class Config(settings: Settings) {

  type Instructions = List[String]

  def init: Instructions = List(
    setoption("Hash", settings.AiStockfishHashSize),
    setoption("Threads", 8),
    setoption("Ponder", false),
    setoption("Aggressiveness", settings.AiStockfishAggressiveness) // 0 - 200
  )

  def game(play: Play): Instructions = List(
    setoption("Skill Level", skill(play.level)),
    setoption("UCI_Chess960", play.chess960)
  )

  def maxMoveTime = 500

  def moveTime(level: Int): Int = 
    maxMoveTime / (level >= 1 && level <= 8).fold(9 - level, 8)

  private def skill(level: Int) = 
    (level >= 1 && level <= 8).fold(math.round((level -1) * (20 / 7f)), 0)

  private def setoption(name: String, value: Any) = 
    "setoption name %s value %s".format(name, value)
}
