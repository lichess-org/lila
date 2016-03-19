package lila.perfStat

import lila.common.LightUser
import lila.rating.{ PerfType, Perf, Glicko }
import lila.user.User

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

final class JsonView(getLightUser: String => Option[LightUser]) {

  def apply(
    user: User,
    stat: PerfStat,
    rank: Option[Int],
    ratingDistribution: Option[List[Int]]) = Json.obj(
    "user" -> user,
    "perf" -> user.perfs(stat.perfType),
    "rank" -> rank,
    "percentile" -> ratingDistribution.map { distrib =>
      lila.user.Stat.percentile(distrib, user.perfs(stat.perfType).intRating) match {
        case (under, sum) => Math.round(under * 1000.0 / sum) / 10.0
      }
    },
    "stat" -> stat.copy(playStreak = stat.playStreak.checkCurrent))

  private def truncate(v: Double) = lila.common.Maths.truncateAt(v, 2)

  private val isoFormatter = ISODateTimeFormat.dateTime
  private implicit val dateWriter: Writes[DateTime] = Writes { d =>
    JsString(isoFormatter print d)
  }
  private implicit val userWriter: OWrites[User] = OWrites { u =>
    Json.obj("name" -> u.username)
  }
  private implicit val glickoWriter: OWrites[Glicko] = OWrites { p =>
    Json.obj(
      "rating" -> truncate(p.rating),
      "deviation" -> truncate(p.deviation),
      "volatility" -> truncate(p.volatility),
      "provisional" -> p.provisional)
  }
  private implicit val perfWriter: OWrites[Perf] = OWrites { p =>
    Json.obj("glicko" -> p.glicko, "nb" -> p.nb, "progress" -> p.progress)
  }
  private implicit val avgWriter: Writes[Avg] = Writes { a =>
    JsNumber(truncate(a.avg))
  }
  private implicit val perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "key" -> pt.key,
      "name" -> pt.name)
  }
  private implicit val userIdWriter: OWrites[UserId] = OWrites { u =>
    val light = getLightUser(u.value)
    Json.obj(
      "id" -> u.value,
      "name" -> light.fold(u.value)(_.name),
      "title" -> light.flatMap(_.title))
  }
  private implicit val ratingAtWrites = Json.writes[RatingAt]
  private implicit val resultWrites = Json.writes[Result]
  private implicit val resultsWrites = Json.writes[Results]
  private implicit val streakWrites = Json.writes[Streak]
  private implicit val streaksWrites = Json.writes[Streaks]
  private implicit val playStreakWrites = Json.writes[PlayStreak]
  private implicit val resultStreakWrites = Json.writes[ResultStreak]
  private implicit val countWrites = Json.writes[Count]
  private implicit val perfStatWrites = Json.writes[PerfStat]
}
