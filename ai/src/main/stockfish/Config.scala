package lila.ai
package stockfish

import model._

private[ai] final class Config(
    hashSize: Int,
    nbThreads: Int,
    playMaxMoveTime: Int,
    analyseMoveTime: Int,
    val debug: Boolean) {

  import Config._

  def moveTime(level: Int) = (levelBox(level) * playMaxMoveTime) / levels.end

  def ownBook(level: Int) = levelBox(level) > 4

  def skill(level: Int) = math.round((levelBox(level) - 1) * (skillMax / 7f))

  def depth(level: Int): Option[Int] = Map(
    1 -> 1,
    2 -> 2,
    3 -> 3,
    4 -> 4,
    5 -> 6,
    6 -> 8,
    7 -> 10
  // 8 -> inf
  ) get levelBox(level)

  def init = List(
    setoption("Hash", hashSize),
    setoption("Threads", nbThreads),
    setoption("Ponder", false))

  def prepare(task: Task) = task.fold(
    play ⇒ List(
      setoption("Uci_AnalyseMode", false),
      setoption("Skill Level", skill(play.level)),
      setoption("UCI_Chess960", play.chess960),
      setoption("OwnBook", ownBook(play.level)),
      "isready"),
    anal ⇒ List(
      setoption("Uci_AnalyseMode", true),
      setoption("Skill Level", skillMax),
      setoption("UCI_Chess960", anal.chess960),
      setoption("OwnBook", true),
      "isready"))

  def go(task: Task): List[String] = task.fold(
    play ⇒ List(
      position(play.fen, play.moves),
      "go movetime %d%s".format(
        moveTime(play.level),
        ~depth(play.level).map(" depth " + _)
      )),
    anal ⇒ List(
      position(anal.fen, anal.pastMoves),
      "go movetime %d".format(analyseMoveTime)))

  private def position(fen: Option[String], moves: String) =
    "position %s moves %s".format(fen.fold("startpos")("fen " + _), moves)

  private def setoption(name: String, value: Any) =
    "setoption name %s value %s".format(name, value)
}

object Config {

  val levels = 1 to 8

  val levelBox = intBox(1 to 8) _

  val skillMax = 20
}
