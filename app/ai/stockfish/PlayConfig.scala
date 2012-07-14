package lila
package ai.stockfish

import model._
import model.play._
import core.Settings

final class PlayConfig(settings: Settings) extends Config {

  import Config.levelBox

  def moveTime(level: Int) = maxMoveTime / (9 - levelBox(level))

  def ownBook(level: Int) = levelBox(level) > 4

  def skill(level: Int) = math.round((levelBox(level) -1) * (20 / 7f))

  def depth(level: Int) = levelBox(level)

  def init = List(
    setoption("Hash", settings.AiStockfishPlayHashSize),
    setoption("Threads", 8),
    setoption("Ponder", false),
    setoption("Aggressiveness", settings.AiStockfishPlayAggressiveness), 
    setoption("Cowardice", settings.AiStockfishPlayCowardice) 
  )

  def game(play: Play) = List(
    setoption("Skill Level", skill(play.level)),
    setoption("UCI_Chess960", play.chess960),
    setoption("OwnBook", ownBook(play.level)),
    "ucinewgame",
    "isready"
  )

  def move(fen: Option[String], moves: String, level: Int) = List(
    "position %s moves %s".format(fen.fold("fen " + _, "startpos"), moves),
    "go movetime %d depth %d".format(moveTime(level), depth(level))
  )

  private def maxMoveTime = settings.AiStockfishPlayMaxMoveTime
}
