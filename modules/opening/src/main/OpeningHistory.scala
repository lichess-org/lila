package lila.opening

import play.api.libs.json.{ JsArray, Json, Reads, Writes }

case class OpeningHistorySegment[A](
    month: String, // YYYY-MM
    black: A,
    draws: A,
    white: A
)(implicit num: Numeric[A]) {

  import num.mkNumericOps

  def isEmpty = black == 0 || draws == 0 || white == 0

  lazy val sum: A = black + draws + white
}

object OpeningHistory {

  implicit def segmentJsonRead[A: Reads: Numeric]   = Json.reads[OpeningHistorySegment[A]]
  implicit def segmentJsonWrite[A: Writes: Numeric] = Json.writes[OpeningHistorySegment[A]]
}
