package lidraughts.challenge

import play.api.libs.json._
import play.api.i18n.Lang

import lidraughts.socket.UserLagCache
import lidraughts.i18n.{ I18nKeys => trans }

final class JsonView(
    getLightUser: lidraughts.common.LightUser.GetterSync,
    isOnline: lidraughts.user.User.ID => Boolean
) {

  import Challenge._

  def apply(a: AllChallenges, lang: Lang): JsObject = Json.obj(
    "in" -> a.in.map(apply(Direction.In.some)),
    "out" -> a.out.map(apply(Direction.Out.some)),
    "i18n" -> translations(lang)
  )

  def show(challenge: Challenge, socketVersion: Int, direction: Option[Direction]) = Json.obj(
    "challenge" -> apply(direction)(challenge),
    "socketVersion" -> socketVersion
  )

  private def apply(direction: Option[Direction])(c: Challenge): JsObject = Json.obj(
    "id" -> c.id,
    "direction" -> direction.map(_.name),
    "status" -> c.status.name,
    "challenger" -> c.challengerUser,
    "destUser" -> c.destUser,
    "variant" -> Json.obj(
      "key" -> c.variant.key,
      "short" -> c.variant.shortName,
      "name" -> c.variant.name
    ),
    "initialFen" -> c.initialFen,
    "rated" -> c.mode.rated,
    "timeControl" -> (c.timeControl match {
      case c @ TimeControl.Clock(clock) => Json.obj(
        "type" -> "clock",
        "limit" -> clock.limitSeconds,
        "increment" -> clock.incrementSeconds,
        "show" -> clock.show
      )
      case TimeControl.Correspondence(d) => Json.obj(
        "type" -> "correspondence",
        "daysPerTurn" -> d
      )
      case TimeControl.Unlimited => Json.obj("type" -> "unlimited")
    }),
    "color" -> c.colorChoice.toString.toLowerCase,
    "perf" -> Json.obj(
      "icon" -> iconChar(c).toString,
      "name" -> c.perfType.name
    )
  )

  private def iconChar(c: Challenge) =
    if (c.variant == draughts.variant.FromPosition) '*'
    else c.perfType.iconChar

  private implicit val RegisteredWrites = OWrites[Registered] { r =>
    val light = getLightUser(r.id)
    Json.obj(
      "id" -> r.id,
      "name" -> light.fold(r.id)(_.name),
      "title" -> light.map(_.title),
      "rating" -> r.rating.int
    ).add("provisional" -> r.rating.provisional)
      .add("patron" -> light.??(_.isPatron))
      .add("online" -> isOnline(r.id))
      .add("lag" -> UserLagCache.getLagRating(r.id))
  }

  private def translations(lang: Lang) = lidraughts.i18n.JsDump.keysToObject(List(
    trans.rated,
    trans.casual,
    trans.waiting,
    trans.accept,
    trans.decline,
    trans.viewInFullSize,
    trans.cancel
  ), lidraughts.i18n.I18nDb.Site, lang)
}
