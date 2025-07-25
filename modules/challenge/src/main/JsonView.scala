package lila.challenge
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.i18n.Translate
import lila.core.id.GameFullId
import lila.core.socket.{ SocketVersion, userLag }
import lila.game.JsonView.given
import lila.ui.Icon

final class JsonView(
    baseUrl: lila.core.config.BaseUrl,
    getLightUser: lila.core.LightUser.GetterSync,
    getLagRating: userLag.GetLagRating,
    isOnline: lila.core.socket.IsOnline
):

  import Challenge.*

  private given OWrites[Challenger.Registered] = OWrites: r =>
    val light = getLightUser(r.id)
    Json
      .obj(
        "id" -> r.id,
        "name" -> light.fold(r.id.into(UserName))(_.name),
        "rating" -> r.rating.int
      )
      .add("title" -> light.map(_.title))
      .add("provisional" -> r.rating.provisional)
      .add("patron" -> light.so(_.isPatron))
      .add("flair" -> light.flatMap(_.flair))
      .add("online" -> isOnline.exec(r.id))
      .add("lag" -> getLagRating(r.id))

  def apply(a: AllChallenges)(using Translate): JsObject =
    Json.obj(
      "in" -> a.in.map(apply(Direction.In.some)),
      "out" -> a.out.map(apply(Direction.Out.some)),
      "reasons" -> JsObject(Challenge.DeclineReason.allExceptBot.map: r =>
        r.key -> JsString(r.trans.txt()))
    )

  def websiteAndLichobile(
      challenge: Challenge,
      socketVersion: SocketVersion,
      direction: Option[Direction]
  )(using Translate) =
    Json.obj(
      "challenge" -> apply(direction)(challenge),
      "socketVersion" -> socketVersion
    )

  def apiAndMobile(
      challenge: Challenge,
      socketVersion: Option[SocketVersion],
      direction: Option[Direction],
      fullId: Option[GameFullId] = none
  )(using Translate) =
    apply(direction)(challenge)
      .add("socketVersion" -> socketVersion)
      .add("fullId" -> fullId)

  private given OWrites[Challenge.Open] = Json.writes

  def apply(direction: Option[Direction])(c: Challenge)(using Translate): JsObject =
    Json
      .obj(
        "id" -> c.id,
        "url" -> s"$baseUrl/${c.id}",
        "status" -> c.status.name,
        "challenger" -> c.challengerUser,
        "destUser" -> c.destUser,
        "variant" -> c.variant,
        "rated" -> c.rated,
        "speed" -> c.speed.key,
        "timeControl" -> c.timeControl.match
          case TimeControl.Clock(clock) =>
            Json.obj(
              "type" -> "clock",
              "limit" -> clock.limitSeconds,
              "increment" -> clock.incrementSeconds,
              "show" -> clock.show
            )
          case TimeControl.Correspondence(d) =>
            Json.obj(
              "type" -> "correspondence",
              "daysPerTurn" -> d
            )
          case TimeControl.Unlimited => Json.obj("type" -> "unlimited")
        ,
        "color" -> c.colorChoice.toString.toLowerCase,
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

  private def iconOf(c: Challenge): Icon =
    if c.variant == chess.variant.FromPosition
    then Icon.Feather
    else c.perfType.icon
