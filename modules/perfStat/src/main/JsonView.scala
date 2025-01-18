package lila.perfStat

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._

import lila.common.LightUser
import lila.rating.{ Glicko, Perf, PerfType }
import lila.user.User

final class JsonView(getLightUser: LightUser.GetterSync) {

  import JsonView._

  implicit private val userIdWriter: OWrites[UserId] = OWrites { u =>
    val light = getLightUser(u.value)
    Json.obj(
      "id"    -> u.value,
      "name"  -> light.fold(u.value)(_.name),
      "title" -> light.flatMap(_.title)
    )
  }

  implicit val ratingAtWrites: OWrites[RatingAt]                      = Json.writes[RatingAt]
  implicit val resultWrites: OWrites[Result]                        = Json.writes[Result]
  implicit val resultsWrites: OWrites[Results]                       = Json.writes[Results]
  implicit val streakWrites: OWrites[Streak]                        = Json.writes[Streak]
  implicit val streaksWrites: OWrites[Streaks]                       = Json.writes[Streaks]
  implicit val playStreakWrites: OWrites[PlayStreak]                    = Json.writes[PlayStreak]
  implicit val resultStreakWrites: OWrites[ResultStreak]                  = Json.writes[ResultStreak]
  implicit val countWrites: OWrites[Count]                         = Json.writes[Count]
  implicit def perfStatWrites(implicit lang: Lang): OWrites[PerfStat] = Json.writes[PerfStat]

  def apply(
      user: User,
      stat: PerfStat,
      rank: Option[Int],
      percentile: Option[Double]
  )(implicit lang: Lang) =
    Json.obj(
      "user"       -> user,
      "perf"       -> user.perfs(stat.perfType),
      "rank"       -> rank,
      "percentile" -> percentile,
      "stat"       -> stat
    )
}

object JsonView {

  private def round(v: Double, depth: Int = 2) = lila.common.Maths.roundAt(v, depth)

  private val isoFormatter = ISODateTimeFormat.dateTime
  implicit private val dateWriter: Writes[DateTime] = Writes { d =>
    JsString(isoFormatter print d)
  }
  implicit private val userWriter: OWrites[User] = OWrites { u =>
    Json.obj("name" -> u.username)
  }
  implicit val glickoWriter: OWrites[Glicko] = OWrites { p =>
    Json.obj(
      "rating"      -> round(p.rating),
      "deviation"   -> round(p.deviation),
      "provisional" -> p.provisional
    )
  }
  implicit val perfWriter: OWrites[Perf] = OWrites { p =>
    Json.obj("glicko" -> p.glicko, "nb" -> p.nb, "progress" -> p.progress)
  }
  implicit private val avgWriter: Writes[Avg] = Writes { a =>
    JsNumber(round(a.avg))
  }
  implicit def perfTypeWriter(implicit lang: Lang): OWrites[PerfType] =
    OWrites { pt =>
      Json.obj(
        "key"  -> pt.key,
        "name" -> pt.trans
      )
    }
}
