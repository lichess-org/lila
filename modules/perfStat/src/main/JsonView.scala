package lila.perfStat

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser
import lila.core.i18n.Translate
import lila.rating.{ Glicko, PerfType }

final class JsonView(getLightUser: LightUser.GetterSync):

  import JsonView.given

  private given userIdWriter: OWrites[UserId] = OWrites { u =>
    val light = getLightUser(u)
    Json.obj(
      "id" -> u.value,
      "name" -> light.fold(u.into(UserName))(_.name),
      "title" -> light.flatMap(_.title)
    )
  }

  given Writes[RatingAt] = Json.writes
  given Writes[GameAt] = Json.writes
  given Writes[Result] = Json.writes
  given Writes[Results] = Json.writes
  given Writes[Streak] = Json.writes
  given Writes[Streaks] = Json.writes
  given Writes[PlayStreak] = Json.writes
  given Writes[ResultStreak] = Json.writes
  given Writes[Count] = Json.writes
  given (using Translate): Writes[PerfStat] = Json.writes

  def apply(data: PerfStatData)(using Translate) =
    Json.obj(
      "user" -> data.user.user,
      "perf" -> data.user.perfs(data.stat.perfType),
      "rank" -> data.rank,
      "percentile" -> data.percentile,
      "stat" -> data.stat
    )

object JsonView:

  import lila.rating.Glicko.glickoWrites

  private given Writes[Instant] = Writes: d =>
    JsString(isoDateTimeFormatter.print(d))

  given OWrites[User] = OWrites: u =>
    Json.obj("name" -> u.username)

  given OWrites[Perf] = OWrites: p =>
    Json.obj("glicko" -> p.glicko, "nb" -> p.nb, "progress" -> p.progress)

  private given Writes[Avg] = Writes: a =>
    JsNumber(scalalib.Maths.roundDownAt(a.avg, 2))

  given (using Translate): OWrites[PerfType] = OWrites: pt =>
    Json.obj(
      "key" -> pt.key,
      "name" -> pt.trans
    )
