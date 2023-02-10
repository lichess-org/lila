package lila.msg

import play.api.libs.json.*

import lila.user.User
import lila.common.Json.given
import lila.common.LightUser
import lila.relation.Relations

final class MsgJson(
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline
)(using Executor):

  implicit private val lastMsgWrites: OWrites[Msg.Last]    = Json.writes[Msg.Last]
  implicit private val relationsWrites: OWrites[Relations] = Json.writes[Relations]

  def threads(me: User)(threads: List[MsgThread]): Fu[JsArray] =
    withContacts(me, threads) map { threads =>
      JsArray(threads.map(renderThread(_, me.some)))
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
        "contacts" -> threads.map(renderThread(_, None)),
        "friends"  -> res.friends,
        "users"    -> res.users
      )
    }

  private def withContacts(me: User, threads: List[MsgThread]): Fu[List[MsgThread.WithContact]] =
    lightUserApi.asyncMany(threads.map(_ other me)) map { users =>
      threads.zip(users).map { case (thread, userOption) =>
        MsgThread.WithContact(thread, userOption | LightUser.fallback(thread other me into UserName))
      }
    }

  private def renderThread(t: MsgThread.WithContact, forUser: Option[User]) =
    Json
      .obj(
        "user" -> renderContact(t.contact),
        "lastMsg" -> forUser.fold(t.thread.lastMsg)(me =>
          if t.thread.maskFor.contains(me.id) then t.thread.maskWith.getOrElse(t.thread.lastMsg)
          else t.thread.lastMsg
        )
      )

  private def renderContact(user: LightUser): JsObject =
    LightUser
      .writeNoId(user)
      .add("online" -> isOnline(user.id))
