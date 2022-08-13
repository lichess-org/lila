package lila.opening

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.common.LilaOpening
import lila.common.Markdown

case class OpeningData(
    _id: LilaOpening.Key,
    opening: LilaOpening,
    history: List[OpeningHistorySegment[Int]],
    historyAt: DateTime,
    nbGames: Int,
    markdown: Markdown
) extends OpeningData.Base {
  def key = _id
}

object OpeningData {
  import reactivemongo.api.bson.Macros
  import lila.db.dsl._
  import OpeningHistory.historySegmentsHandler
  implicit val openingDataHandler    = Macros.handler[OpeningData]
  implicit val openingPreviewHandler = Macros.handler[Preview]

  trait Base {
    def key: LilaOpening.Key
    def opening: LilaOpening
    def nbGames: Int
  }

  private[opening] val previewProjection = $doc("opening" -> true, "nbGames" -> true)
  case class Preview(_id: LilaOpening.Key, opening: LilaOpening, nbGames: Int) extends Base {
    def key = _id
  }

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
  def lastMonth =
    DateTimeFormat forPattern "yyyy-MM" print {
      val now = DateTime.now
      if (now.dayOfMonth.get > 7) now else now.minusMonths(1)
    }
}
