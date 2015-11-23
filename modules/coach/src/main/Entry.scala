package lila.coach

import chess.{ Color, Status }
import lila.rating.PerfType
import org.joda.time.DateTime
import scalaz.NonEmptyList

case class Entry(
  userId: String,
  gameId: String,
  color: Color,
  perf: PerfType,
  eco: Ecopening,
  opponent: Opponent,
  cpl: Grouped[Numbers],
  movetime: Grouped[Numbers],
  luck: Grouped[Ratio],
  opportunism: Grouped[Ratio],
  nbMoves: Grouped[Int],
  result: Result,
  status: Status,
  endPhase: Phase,
  ratingDiff: Int,
  analysed: Boolean,
  date: DateTime)

case class Opponent(rating: Int, strength: RelativeStrength)

sealed trait Result
object Result {
  object Win extends Result
  object Draw extends Result
  object Loss extends Result
}

sealed trait Phase
object Phase {
  object Opening extends Phase
  object Middle extends Phase
  object End extends Phase
}

sealed trait RelativeStrength
object RelativeStrength {
  object MuchWeaker extends RelativeStrength
  object Weaker extends RelativeStrength
  object Equal extends RelativeStrength
  object Strong extends RelativeStrength
  object MuchStronger extends RelativeStrength
}

case class ByMovetime[A](values: List[A])

object ByMovetime {
  val uppers = List(10, 30, 50, 100, 200, 6000)
}

case class ByPhase[A](opening: A, middle: Option[A], end: Option[A], all: A)

case class ByPieceRole[A](pawn: A, knight: A, bishop: A, rook: A, queen: A, king: A)

case class ByPositionQuality[A](losing: A, bad: A, equal: A, good: A, winning: A)

case class Grouped[A](
  byPhase: ByPhase[A],
  byMovetime: ByMovetime[A],
  byPieceRole: ByPieceRole[A],
  byPositionQuality: ByPositionQuality[A])

case class Numbers(size: Int, mean: Int, median: Double, sd: Double)

case class Ratio(n: Int, d: Int) // n/d

object Numbers {
  def apply(nbs: List[Int]): Option[Numbers] = nbs match {
    case Nil => none
    case xs  => Numbers(nbs.size, 0, 0, 0).some
  }
}
