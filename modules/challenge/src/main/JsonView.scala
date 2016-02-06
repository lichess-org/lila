package lila.challenge

import play.api.libs.json._

import lila.common.PimpedJson._

final class JsonView(getLightUser: String => Option[lila.common.LightUser]) {

  import Challenge._

  def apply(a: AllChallenges): JsObject = Json.obj(
    "in" -> a.in.map(apply),
    "out" -> a.out.map(apply))

  def show(challenge: Challenge, socketVersion: Int) = Json.obj(
    "challenge" -> apply(challenge),
    "socketVersion" -> socketVersion)

  private def apply(c: Challenge): JsObject = Json.obj(
    "id" -> c.id,
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
      "provisional" -> r.rating.provisional
    ).noNull
  }
}
