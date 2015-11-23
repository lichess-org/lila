package lila.coach

import chess.Color
import lila.rating.PerfType
import org.joda.time.DateTime

case class Entry(
  userId: String,
  gameId: String,
  color: Color,
  perf: PerfType,
  opponentRating: Int,
  cpl: Phases[CPL],
  result: Result,
  date: DateTime)

sealed trait Result
object Result {
  object Win extends Result
  object Draw extends Result
  object Loss extends Result
}

case class MoveTime(millis: Int)

case class CPL(
  average: Int,
  median: Int,
  consistency: Int)

case class Phases[A](
  opening: A,
  middle: Option[A],
  end: Option[A],
  all: A)
