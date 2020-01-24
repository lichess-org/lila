package lila.msg

import play.api.libs.json._

import lila.user.User
import lila.common.Json._
import lila.common.LightUser

final class MsgJson(
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline
) {

  implicit val threadIdWrites: Writes[MsgThread.Id] = Writes.of[String].contramap[MsgThread.Id](_.value)
  implicit val lastMsgWrites: OWrites[Msg.Last]     = Json.writes[Msg.Last]

  def threads(me: User)(threads: List[MsgThread]): Fu[JsArray] =
    lightUserApi.preloadMany(threads.map(_ other me)) inject JsArray(
      threads.map { t =>
        Json.obj(
          "id"      -> t.id,
          "contact" -> contact(t other me),
          "lastMsg" -> t.lastMsg
        )
      }
    )

  private def contact(userId: User.ID): JsObject =
    LightUser.lightUserWrites
      .writes(lightUserApi.sync(userId) | LightUser.fallback(userId))
      .add("online" -> isOnline(userId))
}
