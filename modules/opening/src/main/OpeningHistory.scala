package lila.opening

import play.api.libs.json.{ JsArray, JsString, Json, Reads, Writes }

case class OpeningHistorySegment[A](
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

  implicit def segmentJsonRead[A: Reads: Numeric] = Json.reads[OpeningHistorySegment[A]]

  implicit def segmentJsonWrite[A: Writes: Numeric] = Writes[OpeningHistorySegment[A]] { s =>
    Json.arr(s.black, s.draws, s.white)
  }
}
