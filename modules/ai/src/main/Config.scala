package lila.ai

import scala.concurrent.duration.FiniteDuration

import actorApi._

private[ai] case class Config(
    execPath: String,
    hashSize: Int,
    nbThreads: Int,
    nbInstances: Int,
    playMaxMoveTime: FiniteDuration,
    analyseMoveTime: FiniteDuration,
    playTimeout: FiniteDuration,
    analyseMaxPlies: Int,
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
    7 -> 10,
    8 -> 12
  ) get levelBox(level)

  def init = List(
    setoption("Hash", hashSize),
    setoption("Threads", nbThreads),
    setoption("Ponder", false))

  def prepare(req: Req) = req match {
    case r: PlayReq => List(
      setoption("Uci_AnalyseMode", false),
      setoption("Skill Level", skill(r.level)),
      setoption("UCI_Chess960", r.chess960),
      setoption("OwnBook", ownBook(r.level)))
    case r: AnalReq => List(
      setoption("Uci_AnalyseMode", true),
      setoption("Skill Level", skillMax),
      setoption("UCI_Chess960", r.chess960),
      setoption("OwnBook", true))
  }

  def go(req: Req): List[String] = req match {
    case r: PlayReq => List(
      position(r.fen, r.moves),
      "go movetime %d%s".format(moveTime(r.level), depth(r.level).??(" depth " + _)))
    case r: AnalReq => List(
      position(r.fen, r.moves),
      "go movetime %d".format(analyseMoveTime.toMillis))
  }

  private def position(fen: Option[String], moves: List[String]) =
    "position %s moves %s".format(
      fen.fold("startpos")("fen " + _),
      moves mkString " ")

  private def setoption(name: String, value: Any) =
    "setoption name %s value %s".format(name, value)
}

object Config {

  val levelMax = 8

  val levels = 1 to levelMax

  val levelBox = intBox(levels) _

  val skillMax = 20
}
