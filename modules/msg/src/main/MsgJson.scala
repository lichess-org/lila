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
  implicit val msgIdWrites: Writes[Msg.Id]          = Writes.of[String].contramap[Msg.Id](_.value)
  implicit val lastMsgWrites: OWrites[Msg.Last]     = Json.writes[Msg.Last]

  def threads(me: User)(threads: List[MsgThread]): Fu[JsArray] =
    lightUserApi.preloadMany(threads.map(_ other me)) inject JsArray(
      threads.map { t =>
        Json.obj(
          "id"      -> t.id,
          "contact" -> contactJson(t other me),
          "lastMsg" -> t.lastMsg
        )
      }
    )

  def convoWith(contact: User)(t: MsgThread.WithMsgs): JsObject = Json.obj(
    "thread" -> Json.obj(
      "id"      -> t.thread.id,
      "contact" -> contactJson(contact.id)
    ),
    "msgs" -> t.msgs.map { msg =>
      Json.obj(
        "id"   -> msg.id,
        "text" -> msg.text,
        "user" -> msg.user,
        "date" -> msg.date
      )
    }
  )

  private def contactJson(userId: User.ID): JsObject =
    LightUser.lightUserWrites
      .writes(lightUserApi.sync(userId) | LightUser.fallback(userId))
      .add("online" -> isOnline(userId))
}
