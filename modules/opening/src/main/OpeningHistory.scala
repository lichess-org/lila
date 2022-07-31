package lila.opening

import play.api.libs.json.{ JsArray, Json, Reads, Writes }

case class OpeningHistory(history: Map[String, OpeningHistorySegment]) {

  def perMilOf(all: OpeningHistory) = OpeningHistory(
    history.flatMap { case (date, seg) =>
      all.history get date map seg.perMilOf map (date -> _)
    }
  )

  def filterNotEmpty = copy(history.filterValues(!_.isEmpty))
}

case class OpeningHistorySegment(
    black: Long,
    draws: Long,
    white: Long
) {

  def isEmpty = black == 0 || draws == 0 || white == 0

  def perMilOf(all: OpeningHistorySegment) = OpeningHistorySegment(
    black = (black * 1000 / all.black),
    draws = (draws * 1000 / all.draws),
    white = (white * 1000 / all.white)
  )
}

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
