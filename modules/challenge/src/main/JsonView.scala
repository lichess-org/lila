package lila.challenge

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.common.licon
import lila.game.JsonView.given
import lila.i18n.{ I18nKeys as trans }
import lila.socket.{ SocketVersion, UserLagCache }

final class JsonView(
    baseUrl: lila.common.config.BaseUrl,
    getLightUser: lila.common.LightUser.GetterSync,
    isOnline: lila.socket.IsOnline
):

  import Challenge.*

  private given OWrites[Challenger.Registered] = OWrites { r =>
    val light = getLightUser(r.id)
    Json
      .obj(
        "id"     -> r.id,
        "name"   -> light.fold(r.id into UserName)(_.name),
        "title"  -> light.map(_.title),
        "rating" -> r.rating.int
      )
      .add("provisional" -> r.rating.provisional)
      .add("patron" -> light.so(_.isPatron))
      .add("online" -> isOnline(r.id))
      .add("lag" -> UserLagCache.getLagRating(r.id))
  }

  def apply(a: AllChallenges)(using lang: Lang): JsObject =
    Json.obj(
      "in"   -> a.in.map(apply(Direction.In.some)),
      "out"  -> a.out.map(apply(Direction.Out.some)),
      "i18n" -> lila.i18n.JsDump.keysToObject(i18nKeys, lang),
      "reasons" -> JsObject(Challenge.DeclineReason.allExceptBot.map { r =>
        r.key -> JsString(r.trans.txt())
      })
    )

  def show(challenge: Challenge, socketVersion: SocketVersion, direction: Option[Direction])(using Lang) =
    Json.obj(
      "challenge"     -> apply(direction)(challenge),
      "socketVersion" -> socketVersion
    )

  private given OWrites[Challenge.Open] = Json.writes

  def apply(direction: Option[Direction])(c: Challenge)(using Lang): JsObject =
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
        "timeControl" -> c.timeControl.match
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
        ,
        "color"      -> c.colorChoice.toString.toLowerCase,
        "finalColor" -> c.finalColor.toString.toLowerCase,
        "perf" -> Json.obj(
          "icon" -> iconOf(c),
          "name" -> c.perfType.trans
        )
      )
      .add("rematchOf" -> c.rematchOf)
      .add("direction" -> direction.map(_.name))
      .add("initialFen" -> c.initialFen)
      .add("declineReason" -> c.declineReason.map(_.trans.txt()))
      .add("declineReasonKey" -> c.declineReason.map(_.key))
      .add("open" -> c.open)
      .add("rules" -> c.nonEmptyRules)

  private def iconOf(c: Challenge): licon.Icon =
    if c.variant == chess.variant.FromPosition
    then licon.Feather
    else c.perfType.icon

  private val i18nKeys = List(
    trans.rated,
    trans.casual,
    trans.waiting,
    trans.accept,
    trans.decline,
    trans.viewInFullSize,
    trans.cancel
  )
