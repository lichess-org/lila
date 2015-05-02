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
  private implicit val profileWrites: Writes[Profile] = Writes { o =>
    Json.obj(
      "country" -> o.country,
      "countryName" -> o.countryInfo.fold[JsValue](JsNull)(i => JsString(i._2)),
      "location" -> o.location,
      "bio" -> o.bio,
      "firstName" -> o.firstName,
      "lastName" -> o.lastName)
  }
  private implicit val playTimeWrites: Writes[User.PlayTime] = Writes { o =>
    Json.obj(
      "total" -> o.total,
      "tv" -> o.tv)
  }

  def apply(u: User, extended: Boolean) = Json.obj(
    "id" -> u.id,
    "username" -> u.username
  ) ++ extended.??(Json.obj(
      "title" -> u.title,
      "online" -> isOnline(u.id),
      "engine" -> u.engine,
      "language" -> u.lang,
      "profile" -> u.profile,
      "perfs" -> u.perfs,
      "createdAt" -> u.createdAt,
      "seenAt" -> u.seenAt,
      "playTime" -> u.playTime
    )).noNull
}
