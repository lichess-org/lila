package lila.history

import scala.concurrent.duration._
import scala.math.round

import org.joda.time.DateTime
import play.api.libs.json._

import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, Perfs }

final class RatingChartApi(
    historyApi: HistoryApi,
    mongoCache: lila.memo.MongoCache.Builder,
    cacheTtl: FiniteDuration) {

  def apply(user: User): Fu[Option[String]] = cache(user) map { chart =>
    chart.nonEmpty option chart
  }

  private val cache = mongoCache[User, String](
    prefix = "history:rating",
    f = (user: User) => build(user) map (~_),
    maxCapacity = 64,
    timeToLive = cacheTtl,
    keyToString = _.id)

  private val columns = Json stringify {
    Json.arr(
      Json.arr("string", "Date"),
      Json.arr("number", "Standard"),
      Json.arr("number", "Opponent Rating"),
      Json.arr("number", "Average")
    )
  }

  private def build(user: User): Fu[Option[String]] = {

    def ratingsMapToJson(perfType: PerfType, ratingsMap: RatingsMap) = ratingsMap.nonEmpty option {
      Json.obj(
        "name" -> perfType.name,
        "points" -> ratingsMap.map {
          case (days, rating) =>
            val date = user.createdAt plusDays days
            Json.arr(date.getYear, date.getMonthOfYear - 1, date.getDayOfMonth, rating)
        }
      )
    }

    historyApi get user.id map2 { (history: History) =>
      Json stringify {
        Json.toJson {
          import lila.rating.PerfType._
          List(Bullet, Blitz, Classical, Correspondence, Chess960, KingOfTheHill, ThreeCheck, Puzzle).map { pt =>
            pt -> history(pt)
          } sortBy (-_._2.size) map {
            case (pt, rm) => ratingsMapToJson(pt, rm)
          } flatten
        }
      }
    }
  }
}
