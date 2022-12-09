package lila.perfStat

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.common.LightUser
import lila.rating.{ Glicko, Perf, PerfType }
import lila.user.User

final class JsonView(getLightUser: LightUser.GetterSync):

  import JsonView.{ given, * }

  private given userIdWriter: OWrites[UserId] = OWrites { u =>
    val light = getLightUser(u)
    Json.obj(
      "id"    -> u.value,
      "name"  -> light.fold(u into UserName)(_.name),
      "title" -> light.flatMap(_.title)
    )
  }

  given Writes[RatingAt]               = Json.writes
  given Writes[GameAt]                 = Json.writes
  given Writes[Result]                 = Json.writes
  given Writes[Results]                = Json.writes
  given Writes[Streak]                 = Json.writes
  given Writes[Streaks]                = Json.writes
  given Writes[PlayStreak]             = Json.writes
  given Writes[ResultStreak]           = Json.writes
  given Writes[Count]                  = Json.writes
  given (using Lang): Writes[PerfStat] = Json.writes

  def apply(data: PerfStatData)(using lang: Lang) =
    Json.obj(
      "user"       -> data.user,
      "perf"       -> data.user.perfs(data.stat.perfType),
      "rank"       -> data.rank,
      "percentile" -> data.percentile,
      "stat"       -> data.stat
    )

object JsonView:

  private def round(v: Double, depth: Int = 2) = lila.common.Maths.roundDownAt(v, depth)

  private val isoFormatter = ISODateTimeFormat.dateTime
  private given Writes[DateTime] = Writes { d =>
    JsString(isoFormatter print d)
  }
  given OWrites[User] = OWrites { u =>
    Json.obj("name" -> u.username)
  }
  given OWrites[Glicko] = OWrites { p =>
    Json.obj(
      "rating"      -> round(p.rating),
      "deviation"   -> round(p.deviation),
      "provisional" -> p.provisional
    )
  }
  given OWrites[Perf] = OWrites { p =>
    Json.obj("glicko" -> p.glicko, "nb" -> p.nb, "progress" -> p.progress)
  }
  private given Writes[Avg] = Writes { a =>
    JsNumber(round(a.avg))
  }
  given (using lang: Lang): OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "key"  -> pt.key,
      "name" -> pt.trans
    )
  }
