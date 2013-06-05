package lila.ai
package stockfish

import model._
import actorApi._
import scala.concurrent.duration.FiniteDuration

private[ai] case class Config(
    execPath: String,
    hashSize: Int,
    nbThreads: Int,
    playMaxMoveTime: FiniteDuration,
    analyseMoveTime: FiniteDuration,
    playTimeout: FiniteDuration,
    analyseTimeout: FiniteDuration,
    debug: Boolean) {

  import Config._

  def moveTime(level: Int) = (levelBox(level) * playMaxMoveTime.toMillis) / levels.end

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

  def prepare(req: Req) = req.analyse.fold(
    List(
      setoption("Uci_AnalyseMode", true),
      setoption("Skill Level", skillMax),
      setoption("UCI_Chess960", req.chess960),
      setoption("OwnBook", true)),
    List(
      setoption("Uci_AnalyseMode", false),
      setoption("Skill Level", skill(req.level)),
      setoption("UCI_Chess960", req.chess960),
      setoption("OwnBook", ownBook(req.level))))

  def go(req: Req): List[String] = req.analyse.fold(
    List(
      position(req.fen, req.moves),
      "go movetime %d".format(analyseMoveTime.toMillis)),
    List(
      position(req.fen, req.moves),
      "go movetime %d%s".format(
        moveTime(req.level),
        ~depth(req.level).map(" depth " + _)
      )))

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
