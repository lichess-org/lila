package lila.challenge

import play.api.libs.json._

import lila.common.PimpedJson._

final class JsonView(getLightUser: String => Option[lila.common.LightUser]) {

  import Challenge._

  def apply(a: AllChallenges): JsObject = Json.obj(
    "in" -> a.in.map(apply(Direction.In.some)),
    "out" -> a.out.map(apply(Direction.Out.some)))

  def show(challenge: Challenge, socketVersion: Int, direction: Option[Direction]) = Json.obj(
    "challenge" -> apply(direction)(challenge),
    "socketVersion" -> socketVersion)

  private def apply(direction: Option[Direction])(c: Challenge): JsObject = Json.obj(
    "id" -> c.id,
    "direction" -> direction.map(_.name),
    "status" -> c.status.name,
    "challenger" -> c.challengerUser,
    "destUser" -> c.destUser,
    "variant" -> Json.obj(
      "key" -> c.variant.key,
      "short" -> c.variant.shortName,
      "name" -> c.variant.name),
    "initialFen" -> c.initialFen,
    "rated" -> c.mode.rated,
    "timeControl" -> (c.timeControl match {
      case c@TimeControl.Clock(l, i) => Json.obj(
        "type" -> "clock",
        "limit" -> l,
        "increment" -> i,
        "show" -> c.show)
      case TimeControl.Correspondence(d) => Json.obj(
        "type" -> "correspondence",
        "daysPerTurn" -> d)
      case TimeControl.Unlimited => Json.obj("type" -> "unlimited")
    }),
    "color" -> c.colorChoice.toString.toLowerCase,
    "perf" -> Json.obj(
      "icon" -> iconChar(c).toString,
      "name" -> c.perfType.name)
  )

  private def iconChar(c: Challenge) =
    if (c.variant == chess.variant.FromPosition) '*'
    else c.perfType.iconChar

  private implicit val RegisteredWrites = OWrites[Registered] { r =>
    val light = getLightUser(r.id)
    Json.obj(
      "id" -> r.id,
      "name" -> light.fold(r.id)(_.name),
      "title" -> light.map(_.title),
      "rating" -> r.rating.int,
      "provisional" -> r.rating.provisional,
      "patron" -> light.??(_.isPatron).option(true)
    ).noNull
  }
}
