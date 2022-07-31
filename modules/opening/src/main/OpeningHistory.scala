package lila.opening

import play.api.libs.json.{ JsArray, Json, Reads, Writes }

case class OpeningHistory(history: List[OpeningHistorySegment]) {

  def perMilOf(all: OpeningHistory) = OpeningHistory(
    history zip all.history map { case (mine, all) =>
      mine perMilOf all
    }
  )

  def filterNotEmpty = copy(history.filter(!_.isEmpty))
}

case class OpeningHistorySegment(
    month: String, // YYYY-MM
    black: Long,
    draws: Long,
    white: Long
) {

  def isEmpty = black == 0 || draws == 0 || white == 0

  def perMilOf(all: OpeningHistorySegment) = copy(
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
    JsArray(h.history.toList map segmentJsonWrite.writes)
  }
}
