package lila.analyse

import chess.format.Uci

case class Evaluation(
    score: Option[Score],
    mate: Option[Int],
    line: List[String]) {

  def checkMate = mate == Some(0)

  def invalid = score.isEmpty && mate.isEmpty

  override def toString = s"Evaluation ${score.fold("?")(_.showPawns)} ${mate | 0} ${line.mkString(" ")}"
}

object Evaluation {

  val start = Evaluation(Score(15).some, none, Nil)
  val empty = Evaluation(none, none, Nil)

  def toInfos(evals: List[Evaluation], moves: List[String], startedAtPly: Int): List[Info] =
    (evals filterNot (_.checkMate) sliding 2).toList.zip(moves).zipWithIndex map {
      case ((List(before, after), move), index) => {
        val variation = before.line match {
          case first :: rest if first != move => first :: rest
          case _                              => Nil
        }
        val best = variation.headOption flatMap Uci.Move.apply
        Info(
          ply = index + 1 + startedAtPly,
          score = after.score,
          mate = after.mate,
          variation = variation,
          best = best) |> { info =>
            if (info.ply % 2 == 1) info.invert else info
          }
      }
    }
}

