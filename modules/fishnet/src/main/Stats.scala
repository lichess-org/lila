package lila.fishnet

case class Stats(
  move: Stats.Result,
  analysis: Stats.Result)

object Stats {

  case class Result(
    acquire: Int,
    success: Int,
    failure: Int)
}
