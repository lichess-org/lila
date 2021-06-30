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

  implicit val ratingAtWrites                      = Json.writes[RatingAt]
  implicit val gameAtWrites                        = Json.writes[GameAt]
  implicit val resultWrites                        = Json.writes[Result]
  implicit val resultsWrites                       = Json.writes[Results]
  implicit val streakWrites                        = Json.writes[Streak]
  implicit val streaksWrites                       = Json.writes[Streaks]
  implicit val playStreakWrites                    = Json.writes[PlayStreak]
  implicit val resultStreakWrites                  = Json.writes[ResultStreak]
  implicit val countWrites                         = Json.writes[Count]
  implicit def perfStatWrites(implicit lang: Lang) = Json.writes[PerfStat]

  def apply(data: PerfStatData)(implicit lang: Lang) =
    Json.obj(
      "user"       -> data.user,
      "perf"       -> data.user.perfs(data.stat.perfType),
      "rank"       -> data.rank,
      "percentile" -> data.percentile,
      "stat"       -> data.stat
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
