package lila.fishnet

case class Stats(
  move: Stats.Result,
  analysis: Stats.Result)

object Stats {

  def emptyResult = Result(0, 0, 0)

  def empty = Stats(emptyResult, emptyResult)

  case class Result(
    acquire: Int,
    success: Int,
    failure: Int)
}
