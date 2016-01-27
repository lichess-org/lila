package lila.challenge

import play.api.libs.json._

object JsonView {

  import Challenge._

  def apply(c: Challenge) = Json.obj(
    "id" -> c.id,
    "challenger" -> c.challenger.map { u =>
      Json.obj(
        "id" -> u.id,
        "name" -> u.name,
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
