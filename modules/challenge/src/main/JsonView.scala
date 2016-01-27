package lila.challenge

import play.api.libs.json._

final class JsonView(getLightUser: String => Option[lila.common.LightUser]) {

  import Challenge._

  def all(in: List[Challenge], out: List[Challenge]) = Json.obj(
    "in" -> in.map(apply),
    "out" -> out.map(apply))

  def apply(c: Challenge) = Json.obj(
    "id" -> c.id,
    "challenger" -> c.challenger.right.toOption.map { u =>
      val light = getLightUser(u.id)
      Json.obj(
        "id" -> u.id,
        "name" -> light.fold(u.id)(_.name),
        "title" -> light.map(_.title),
        "rating" -> u.rating
      )
    },
    "destUserId" -> c.destUserId,
    "variant" -> Json.obj(
      "key" -> c.variant.key,
      "short" -> c.variant.shortName,
      "name" -> c.variant.name),
    "rated" -> c.mode.rated,
    "timeControl" -> (c.timeControl match {
      case TimeControl.Clock(l, i) => Json.obj(
        "type" -> "clock",
        "limit" -> l,
        "increment" -> i)
      case TimeControl.Correspondence(d) => Json.obj(
        "type" -> "clock",
        "daysPerTurn" -> d)
      case TimeControl.Unlimited => Json.obj("type" -> "unlimited")
    }),
    "color" -> c.color.toString.toLowerCase,
    "perf" -> Json.obj(
      "icon" -> c.perfType.iconChar.toString,
      "name" -> c.perfType.name)
  )
}
