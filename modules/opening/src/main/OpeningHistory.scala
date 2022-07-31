package lila.opening

import play.api.libs.json.{ JsArray, Json, Reads, Writes }

case class OpeningHistorySegment[A](
    month: String, // YYYY-MM
    black: A,
    draws: A,
    white: A
)(implicit num: Numeric[A]) {

  import num.{ fromInt, mkNumericOps }

  def isEmpty = black == 0 || draws == 0 || white == 0

  lazy val sum: A = black + draws + white

  def whitePercent            = percentOf(white)
  def blackPercent            = percentOf(black)
  def drawPercent             = percentOf(draws)
  private def percentOf(v: A) = (v.toDouble * 100d / sum.toDouble).toFloat
}

object OpeningHistory {

  implicit def segmentJsonRead[A: Reads: Numeric]   = Json.reads[OpeningHistorySegment[A]]
  implicit def segmentJsonWrite[A: Writes: Numeric] = Json.writes[OpeningHistorySegment[A]]
}
