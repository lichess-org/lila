package lila.fide

import reactivemongo.api.bson.Macros.Annotations.Key
import java.time.YearMonth
import play.api.libs.json.*
import monocle.syntax.all.*
import chess.{ FideId, FideTC }
import chess.rating.Elo

case class FideRatingHistory(
    @Key("_id") id: FideId,
    standard: FideRatingHistory.RatingPoints,
    rapid: FideRatingHistory.RatingPoints,
    blitz: FideRatingHistory.RatingPoints
):
  import FideRatingHistory.RatingPoints

  def focusTC(tc: FideTC) = tc match
    case FideTC.standard => this.focus(_.standard)
    case FideTC.rapid => this.focus(_.rapid)
    case FideTC.blitz => this.focus(_.blitz)

  def set(date: YearMonth, tc: FideTC, elo: Elo): FideRatingHistory =
    focusTC(tc).modify(FideRatingHistory.set(_, date, elo))

  def set(date: YearMonth, elos: Map[FideTC, Elo]): FideRatingHistory =
    elos.foldLeft(this):
      case (hist, (tc, elo)) => hist.set(date, tc, elo)

  def allRatings: Map[FideTC, RatingPoints] =
    Map(
      FideTC.standard -> standard,
      FideTC.rapid -> rapid,
      FideTC.blitz -> blitz
    )

  def toJson = Json.toJsObject:
    allRatings.map: (tc, points) =>
      (tc.toString, FideRatingPoint.raw(points))

private opaque type FideRatingPoint = Int // 2021031800
private object FideRatingPoint extends OpaqueInt[FideRatingPoint]:
  def apply(date: YearMonth, elo: Elo): FideRatingPoint =
    (date.toString.replace("-", "") + "%04d".format(elo.value)).toInt
  extension (point: FideRatingPoint)
    def date: YearMonth =
      val dateNum = point.value / 10000
      YearMonth.of(dateNum / 100, dateNum % 100)
    def elo: Elo = Elo(point % 10000)

object FideRatingHistory:

  type RatingPoints = List[FideRatingPoint] // chronological order, oldest first

  def empty(id: FideId): FideRatingHistory = FideRatingHistory(id, Nil, Nil, Nil)

  private def set(points: RatingPoints, at: YearMonth, elo: Elo): RatingPoints =
    val cleaned = points.filterNot(_.date == at)
    compress((FideRatingPoint(at, elo) :: cleaned))

  // keep the first and last of each streak of identical ratings
  private def compress(points: RatingPoints): RatingPoints =
    if points.sizeIs < 3 then points
    else
      val sorted = points.sortBy(FideRatingPoint.raw)
      val middle = sorted
        .sliding(3)
        .foldLeft(List.empty[FideRatingPoint]):
          case (acc, List(a, b, c)) =>
            if a.elo == b.elo && b.elo == c.elo then acc else b :: acc
          case (acc, _) => acc
      sorted.headOption.toList ::: middle.reverse ::: sorted.lastOption.toList
