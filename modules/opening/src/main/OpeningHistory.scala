package lila.opening

import play.api.libs.json.{ JsArray, JsString, Json, Reads, Writes }

case class OpeningHistorySegment[A](
    month: String, // YYYY-MM
    black: A,
    draws: A,
    white: A
)(implicit num: Numeric[A]) {

  import num.{ fromInt, mkNumericOps }

  lazy val sum: A = black + draws + white

  def whitePercent            = percentOf(white)
  def blackPercent            = percentOf(black)
  def drawPercent             = percentOf(draws)
  private def percentOf(v: A) = (v.toDouble * 100d / sum.toDouble).toFloat
}

object OpeningHistory {

  type Segments = List[OpeningHistorySegment[Int]]

  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit def historySegmentsHandler: BSONHandler[Segments] =
    implicitly[BSONHandler[List[List[Int]]]]
      .as[Segments](
        arr =>
          arr.zipWithIndex map { case (arr, i) =>
            OpeningHistorySegment(
              month = f"${OpeningData.firstYear + math.floor(i / 12).toInt}-${(i % 12) + 1}%02.0f",
              black = arr(0),
              draws = arr(1),
              white = arr(2)
            )
          },
        segs => segs map { s => List(s.black, s.draws, s.white) }
      )

  implicit def segmentJsonRead[A: Reads: Numeric]   = Json.reads[OpeningHistorySegment[A]]
  implicit def segmentJsonWrite[A: Writes: Numeric] = Json.writes[OpeningHistorySegment[A]]
}
