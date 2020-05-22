package lila.msg

import play.api.libs.json._

import lila.user.User
import lila.common.Json._
import lila.common.LightUser
import lila.relation.Relations

final class MsgJson(
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val lastMsgWrites: OWrites[Msg.Last]    = Json.writes[Msg.Last]
  implicit private val relationsWrites: OWrites[Relations] = Json.writes[Relations]

  def threads(me: User)(threads: List[MsgThread]): Fu[JsArray] =
    withContacts(me, threads) map { threads =>
      JsArray(threads map renderThread)
    }

  def convo(c: MsgConvo): JsObject =
    Json.obj(
      "user"      -> renderContact(c.contact),
      "msgs"      -> c.msgs.map(renderMsg),
      "relations" -> c.relations,
      "postable"  -> c.postable
    )

  def renderMsg(msg: Msg): JsObject =
    Json
      .obj(
        "text" -> msg.text,
        "user" -> msg.user,
        "date" -> msg.date
      )

  def searchResult(me: User)(res: MsgSearch.Result): Fu[JsObject] =
    withContacts(me, res.threads) map { threads =>
      Json.obj(
        "contacts" -> threads.map(renderThread),
        "friends"  -> res.friends,
        "users"    -> res.users
      )
    }

  private def withContacts(me: User, threads: List[MsgThread]): Fu[List[MsgThread.WithContact]] =
    lightUserApi.asyncMany(threads.map(_ other me)) map { users =>
      threads.zip(users).map {
        case (thread, userOption) =>
          MsgThread.WithContact(thread, userOption | LightUser.fallback(thread other me))
      }
    }

  private def renderThread(t: MsgThread.WithContact) =
    Json
      .obj(
        "user"    -> renderContact(t.contact),
        "lastMsg" -> t.thread.lastMsg
      )

  private def renderContact(user: LightUser): JsObject =
    LightUser
      .writeNoId(user)
      .add("online" -> isOnline(user.id))
}
