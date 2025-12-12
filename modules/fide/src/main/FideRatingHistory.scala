package lila.fide

import reactivemongo.api.bson.Macros.Annotations.Key
import java.time.YearMonth
import monocle.syntax.all.*
import chess.{ FideId, FideTC }
import chess.rating.Elo

case class FideRatingHistory(
    @Key("_id") id: FideId,
    standard: FideRatingHistory.RatingPoints,
    rapid: FideRatingHistory.RatingPoints,
    blitz: FideRatingHistory.RatingPoints
):
  def focusTC(tc: FideTC) = tc match
    case FideTC.standard => this.focus(_.standard)
    case FideTC.rapid => this.focus(_.rapid)
    case FideTC.blitz => this.focus(_.blitz)

  def set(date: YearMonth, tc: FideTC, elo: Elo): FideRatingHistory =
    focusTC(tc).modify(FideRatingHistory.set(_, date, elo))

  def set(date: YearMonth, elos: Map[FideTC, Elo]): FideRatingHistory =
    elos.foldLeft(this):
      case (hist, (tc, elo)) => hist.set(date, tc, elo)

  def compress =
    FideTC.values.foldLeft(this): (hist, tc) =>
      hist.focusTC(tc).modify(FideRatingHistory.compress)

object FideRatingHistory:

  type RatingPoint = (YearMonth, Elo)
  type RatingPoints = List[RatingPoint] // chronological order, oldest first

  def empty(id: FideId): FideRatingHistory = FideRatingHistory(id, Nil, Nil, Nil)

  private def set(points: RatingPoints, date: YearMonth, elo: Elo): RatingPoints =
    val cleaned = points.filterNot(_._1 == date)
    compress(((date -> elo) :: cleaned).sortBy(_._1))

  // keep the first and last of each streak of identical ratings
  private def compress(points: RatingPoints): RatingPoints =
    if points.sizeIs < 3 then points
    else
      val middle = points
        .sliding(3)
        .foldLeft(List.empty[RatingPoint]):
          case (acc, List(a, b, c)) =>
            if a._2 == b._2 && b._2 == c._2 then acc else b :: acc
          case (acc, _) => acc
      points.headOption.toList ::: middle.reverse ::: points.lastOption.toList
