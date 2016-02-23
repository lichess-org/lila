package lila.user

import lila.common.PimpedJson._
import lila.rating.{ Perf, Glicko, PerfType }
import play.api.libs.json._
import User.PlayTime

final class JsonView(isOnline: String => Boolean) {

  private implicit val perfWrites: Writes[Perf] = Writes { o =>
    Json.obj(
      "games" -> o.nb,
      "rating" -> o.glicko.rating.toInt,
      "rd" -> o.glicko.deviation.toInt,
      "prog" -> o.progress)
  }
  private implicit val profileWrites = Json.writes[Profile]
  private implicit val playTimeWrites = Json.writes[PlayTime]

  def apply(u: User, onlyPerf: Option[PerfType] = None) = Json.obj(
    "id" -> u.id,
    "username" -> u.username,
    "title" -> u.title,
    "online" -> isOnline(u.id),
    "engine" -> u.engine,
    "booster" -> u.booster,
    "language" -> u.lang,
    "profile" -> u.profile.??(profileWrites.writes).noNull,
    "perfs" -> JsObject(u.perfs.perfsMap collect {
      case (key, perf) if onlyPerf.fold(true)(_.key == key) => key -> perfWrites.writes(perf)
    }),
    "createdAt" -> u.createdAt,
    "seenAt" -> u.seenAt,
    "playTime" -> u.playTime
  ).noNull
}

object JsonView {

  implicit val nameWrites = Writes[User] { u =>
    JsString(u.username)
  }
}
