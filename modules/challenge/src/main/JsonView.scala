package lila.challenge

import play.api.libs.json._
import play.api.i18n.Lang

import lila.i18n.{ I18nKeys => trans }
import lila.socket.Socket.SocketVersion
import lila.socket.UserLagCache

final class JsonView(
    baseUrl: lila.common.config.BaseUrl,
    getLightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.socket.IsOnline
) {

  import lila.game.JsonView._
  import Challenge._

  implicit private val RegisteredWrites = OWrites[Challenger.Registered] { r =>
    val light = getLightUser(r.id)
    Json
      .obj(
        "id"     -> r.id,
        "name"   -> light.fold(r.id)(_.name),
        "title"  -> light.map(_.title),
        "rating" -> r.rating.int
      )
      .add("provisional" -> r.rating.provisional)
      .add("patron" -> light.??(_.isPatron))
      .add("online" -> isOnline(r.id))
      .add("lag" -> UserLagCache.getLagRating(r.id))
  }

  def apply(a: AllChallenges)(implicit lang: Lang): JsObject =
    Json.obj(
      "in"   -> a.in.map(apply(Direction.In.some)),
      "out"  -> a.out.map(apply(Direction.Out.some)),
      "i18n" -> lila.i18n.JsDump.keysToObject(i18nKeys, lang)
    )

  def show(challenge: Challenge, socketVersion: SocketVersion, direction: Option[Direction])(implicit
      lang: Lang
  ) =
    Json.obj(
      "challenge"     -> apply(direction)(challenge),
      "socketVersion" -> socketVersion
    )

  def apply(direction: Option[Direction])(c: Challenge)(implicit lang: Lang): JsObject =
    Json
      .obj(
        "id"         -> c.id,
        "url"        -> s"$baseUrl/${c.id}",
        "status"     -> c.status.name,
        "challenger" -> c.challengerUser,
        "destUser"   -> c.destUser,
        "variant"    -> c.variant,
        "rated"      -> c.mode.rated,
        "speed"      -> c.speed.key,
        "timeControl" -> (c.timeControl match {
          case TimeControl.Clock(clock) =>
            Json.obj(
              "type"      -> "clock",
              "limit"     -> clock.limitSeconds,
              "increment" -> clock.incrementSeconds,
              "show"      -> clock.show
            )
          case TimeControl.Correspondence(d) =>
            Json.obj(
              "type"        -> "correspondence",
              "daysPerTurn" -> d
            )
          case TimeControl.Unlimited => Json.obj("type" -> "unlimited")
        }),
        "color" -> c.colorChoice.toString.toLowerCase,
        "perf" -> Json.obj(
          "icon" -> iconChar(c).toString,
          "name" -> c.perfType.trans
        )
      )
      .add("direction" -> direction.map(_.name))
      .add("initialFen" -> c.initialFen)
      .add("declineReason" -> c.declineReason.map(_.trans.txt()))

  private def iconChar(c: Challenge) =
    if (c.variant == chess.variant.FromPosition) '*'
    else c.perfType.iconChar

  private val i18nKeys = List(
    trans.rated,
    trans.casual,
    trans.waiting,
    trans.accept,
    trans.decline,
    trans.viewInFullSize,
    trans.cancel
  ).map(_.key)
}
