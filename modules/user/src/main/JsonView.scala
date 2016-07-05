package lila.user

import lila.common.PimpedJson._
import lila.rating.{ Perf, Glicko, PerfType }
import play.api.libs.json._
import User.{ PlayTime, LightPerf }

final class JsonView(isOnline: String => Boolean) {

  import JsonView._
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
    "perfs" -> perfs(u, onlyPerf),
    "createdAt" -> u.createdAt,
    "seenAt" -> u.seenAt,
    "playTime" -> u.playTime
  ).noNull

  def lightPerfIsOnline(lp: LightPerf) = {
    val json = lightPerfWrites.writes(lp)
    if (isOnline(lp.user.id)) json ++ Json.obj("online" -> true)
    else json
  }
}

object JsonView {

  implicit val nameWrites = Writes[User] { u =>
    JsString(u.username)
  }

  implicit val lightPerfWrites = OWrites[LightPerf] { l =>
    Json.obj(
      "id" -> l.user.id,
      "username" -> l.user.name,
      "title" -> l.user.title,
      "perfs" -> Json.obj(
        l.perfKey -> Json.obj("rating" -> l.rating, "progress" -> l.progress))
    ).noNull
  }

  implicit val modWrites = OWrites[User] { u =>
    Json.obj(
      "id" -> u.id,
      "username" -> u.username,
      "title" -> u.title,
      "engine" -> u.engine,
      "booster" -> u.booster,
      "troll" -> u.troll,
      "games" -> u.count.game).noNull
  }

  implicit val perfWrites: OWrites[Perf] = OWrites { o =>
    Json.obj(
      "games" -> o.nb,
      "rating" -> o.glicko.rating.toInt,
      "rd" -> o.glicko.deviation.toInt,
      "prov" -> o.glicko.provisional,
      "prog" -> o.progress)
  }

  def perfs(u: User, onlyPerf: Option[PerfType] = None) =
    JsObject(u.perfs.perfsMap collect {
      case (key, perf) if onlyPerf.fold(true)(_.key == key) => key -> perfWrites.writes(perf)
    })

  def perfs(u: User, onlyPerfs: List[PerfType]) =
    JsObject(onlyPerfs.map { perfType =>
      perfType.key -> perfWrites.writes(u.perfs(perfType))
    })
}
