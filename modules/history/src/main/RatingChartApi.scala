package lila.history

import scala.concurrent.duration._
import scala.math.round

import org.joda.time.DateTime
import play.api.libs.json._

import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, Perfs }

final class RatingChartApi(historyApi: HistoryApi, cacheTtl: Duration) {

  def apply(user: User): Fu[Option[String]] = cache(user)

  private val cache = lila.memo.AsyncCache(build,
    maxCapacity = 50,
    timeToLive = cacheTtl)

  private val columns = Json stringify {
    Json.arr(
      Json.arr("string", "Date"),
      Json.arr("number", "Standard"),
      Json.arr("number", "Opponent Rating"),
      Json.arr("number", "Average")
    )
  }

  private def build(user: User): Fu[Option[String]] = {

    def ratingsMapToJson(key: String, ratingsMap: RatingsMap) = ratingsMap.nonEmpty option {
      Json.obj(
        "name" -> PerfType(key).map(_.name),
        "points" -> ratingsMap.map {
          case (days, rating) =>
            val date = user.createdAt plusDays days
            Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
        }
      )
    }

    historyApi get user.id map2 { (history: History) =>
      Json stringify {
        Json.toJson(List(
          ratingsMapToJson("standard", history.standard),
          ratingsMapToJson("chess960", history.chess960),
          ratingsMapToJson("kingOfTheHill", history.kingOfTheHill),
          ratingsMapToJson("threeCheck", history.threeCheck),
          ratingsMapToJson("bullet", history.bullet),
          ratingsMapToJson("blitz", history.blitz),
          ratingsMapToJson("classical", history.classical),
          ratingsMapToJson("puzzle", history.puzzle)
        ).flatten)
      }
    }
  }
}
