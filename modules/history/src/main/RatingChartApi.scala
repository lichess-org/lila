package lila.history

import scala.concurrent.duration._
import scala.math.round

import org.joda.time.DateTime
import play.api.libs.json._

import lila.rating.Glicko
import lila.user.User

final class RatingChartApi(historyApi: HistoryApi, cacheTtl: Duration) {

  private val cache = lila.memo.AsyncCache(build,
    maxCapacity = 5000,
    timeToLive = cacheTtl)

  private val columns = Json stringify {
    Json.arr(
      Json.arr("string", "Date"),
      Json.arr("number", "Standard"),
      Json.arr("number", "Opponent Rating"),
      Json.arr("number", "Average")
    )
  }

  def apply(user: User): Fu[Option[String]] = cache(user)

  def build(user: User): Fu[Option[String]] = {

    def ratingsMapToJson(name: String, ratingsMap: RatingsMap) = ratingsMap.nonEmpty option {
      Json.obj(
        "name" -> name,
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
          ratingsMapToJson("Standard", history.standard),
          ratingsMapToJson("Chess960", history.chess960),
          ratingsMapToJson("Bullet", history.bullet),
          ratingsMapToJson("Blitz", history.blitz),
          ratingsMapToJson("Classical", history.slow)
        ).flatten.pp)
      }
    }
  }
}
