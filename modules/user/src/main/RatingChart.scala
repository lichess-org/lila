package lila.user

import scala.math.round

import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import play.api.libs.json.Json

import lila.rating.Glicko

case class RatingChart(rows: String) {

  def columns: String = RatingChart.columns
}

object RatingChart {

  private val columns = Json stringify {
    Json.arr(
      Json.arr("string", "Game"),
      Json.arr("number", "Rating"),
      Json.arr("number", "Opponent Rating"),
      Json.arr("number", "Average")
    )
  }

  private[user] def apply(user: User): Fu[Option[String]] = {
    HistoryRepo userRatings user.id map { ratings =>
      val size = ratings.size
      (size > 1) option {
        val rawRatings = (size > 100).fold(
          (size > 200).fold(ratings drop 20, ratings drop 10),
          ratings
        )

        val points = 100
        val ratingMedian = 20
        val opMedian = 20

        val formatter: DateTimeFormatter = DateTimeFormat forPattern "dd/MM/yy"

        def reduce(ratings: List[HistoryEntry]) = {
          val indexed = ratings.toIndexedSeq
          val size = indexed.size
          (size <= points).fold(ratings, {
            val factor = size.toFloat / points
            ((0 until points).toList map { i =>
              indexed(round(i * factor))
            }) :+ indexed.last
          })
        }

        case class Median(entry: HistoryEntry, opMedian: Int)

        def withMedian(ratings: List[HistoryEntry]): List[Median] = {
          val ratingValues = ratings map (_.rating)
          val opValues = ratings map (_.opponent)
          ratings.zipWithIndex map {
            case (entry@HistoryEntry(_, rating, _, opponent), i) => Median(
              entry,
              opValues.slice(i - opMedian, i + opMedian) |> { vs =>
                if (vs.isEmpty) 0 else (vs.sum / vs.size)
              })
          }
        }

        Json stringify {
          val values = withMedian(reduce(rawRatings))
          val ranges = values map { vs =>
            Glicko.range(vs.entry.rating, vs.entry.deviation) match {
              case (x, y) => List(x, y)
            }
          }
          Json.obj(
            "date" -> (values map (x => formatter print x.entry.date)),
            "rating" -> (values map (_.entry.rating)),
            "range" -> ranges,
            // "avg" -> (values map (_.median)),
            "op" -> (values map (_.opMedian))
          )
        }
      }
    }
  }
}
