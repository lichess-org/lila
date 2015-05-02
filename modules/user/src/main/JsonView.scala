package lila.user

import lila.rating.{ Perf, Glicko }
import play.api.libs.json._
import lila.common.PimpedJson._

final class JsonView(isOnline: String => Boolean) {

  private implicit val perfWrites: Writes[Perf] = Writes { o =>
    Json.obj(
      "games" -> o.nb,
      "rating" -> o.glicko.rating.toInt,
      "rd" -> o.glicko.deviation.toInt,
      "prog" -> o.progress)
  }
  private implicit val perfsWrites: Writes[Perfs] = Writes { o =>
    JsObject(o.perfsMap.toList map {
      case (name, perf) => name -> perfWrites.writes(perf)
    })
  }
  private implicit val profileWrites = Json.writes[Profile]

  def apply(u: User, extended: Boolean) = Json.obj(
    "id" -> u.id,
    "username" -> u.username
  ) ++ extended.??(Json.obj(
      "title" -> u.title,
      "online" -> isOnline(u.id),
      "engine" -> u.engine,
      "language" -> u.lang,
      "profile" -> u.profile.??(profileWrites.writes).noNull,
      "perfs" -> u.perfs
    )).noNull
}
