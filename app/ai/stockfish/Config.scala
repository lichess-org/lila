package lila
package ai.stockfish

import model._
import core.Settings

final class Config(settings: Settings) {

  import Config._

  def moveTime(level: Int) = (levelBox(level) * playMaxMoveTime) / levels.end

  def ownBook(level: Int) = levelBox(level) > 4

  def skill(level: Int) = math.round((levelBox(level) - 1) * (skillMax / 7f))

  def depth(level: Int) = levelBox(level)

  def init = List(
    setoption("Hash", settings.AiStockfishHashSize),
    setoption("Threads", settings.AiStockfishThreads),
    setoption("Ponder", false))

  def prepare(task: Task) = task.fold(
    play ⇒ List(
      setoption("Uci_AnalyseMode", false),
      setoption("Skill Level", skill(play.level)),
      setoption("UCI_Chess960", play.chess960),
      setoption("OwnBook", ownBook(play.level)),
      "ucinewgame",
      "isready"),
    anal ⇒ List(
      setoption("Uci_AnalyseMode", true),
      setoption("Skill Level", skillMax),
      setoption("UCI_Chess960", anal.chess960),
      setoption("OwnBook", true),
      "ucinewgame",
      "isready"))

  def go(task: Task) = task.fold(
    play ⇒ List(
      position(play.fen, play.moves),
      "go movetime %d depth %d".format(moveTime(play.level), depth(play.level))),
    anal ⇒ List(
      position(anal.fen, anal.pastMoves),
      "go movetime %d".format(analyseMoveTime)))

  private def playMaxMoveTime = settings.AiStockfishPlayMaxMoveTime

  private def analyseMoveTime = settings.AiStockfishAnalyseMoveTime

  private def position(fen: Option[String], moves: String) =
    "position %s moves %s".format(fen.fold("fen " + _, "startpos"), moves)

  private def setoption(name: String, value: Any) =
    "setoption name %s value %s".format(name, value)
}

object Config {

  val levels = 1 to 8

  val levelBox = intBox(1 to 8) _

  val skillMax = 20
}
