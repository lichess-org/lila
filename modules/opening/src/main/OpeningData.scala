package lila.opening

import org.joda.time.DateTime

import lila.common.LilaOpening
import lila.common.Markdown

case class OpeningData(
    _id: LilaOpening.Key,
    opening: LilaOpening,
    history: List[OpeningHistorySegment[Int]],
    historyAt: DateTime,
    nbGames: Int,
    markdown: Markdown
) {
  def key = _id
}

object OpeningData {
  import reactivemongo.api.bson.Macros
  import lila.db.dsl._
  import OpeningHistory.historySegmentsHandler
  implicit val openingDataHandler = Macros.handler[OpeningData]

  case class WithAll(data: OpeningData, all: List[OpeningHistorySegment[Int]]) {
    lazy val percent = {
      data.history zip all map { case (mine, all) =>
        mine.copy(
          black = (mine.black.toDouble * 100 / all.sum).toFloat,
          draws = (mine.draws.toDouble * 100 / all.sum).toFloat,
          white = (mine.white.toDouble * 100 / all.sum).toFloat
        )
      }
    }
  }

  val firstYear  = 2016
  val firstMonth = s"$firstYear-01"
}
