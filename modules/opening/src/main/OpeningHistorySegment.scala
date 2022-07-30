package lila.opening

import play.api.libs.json.{ JsArray, Json, Reads, Writes }

case class OpeningHistory(history: Map[String, OpeningHistorySegment])

case class OpeningHistorySegment(
    black: Int,
    draws: Int,
    white: Int
)

object OpeningHistory {

  implicit private def segmentJsonRead = Json.reads[OpeningHistorySegment]
  private def segmentJsonWrite         = Json.writes[OpeningHistorySegment]
  implicit def historyJsonRead         = Json.reads[OpeningHistory]
  implicit def historyJsonWrite = Writes[OpeningHistory] { h =>
    JsArray(h.history.toList.sortBy(_._1) map { case (date, s) =>
      Json.obj("date" -> date) ++ segmentJsonWrite.writes(s)
    })
  }
}
