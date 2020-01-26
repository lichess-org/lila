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

  implicit private val threadIdWrites: Writes[MsgThread.Id] =
    Writes.of[String].contramap[MsgThread.Id](_.value)
  implicit private val msgIdWrites: Writes[Msg.Id]         = Writes.of[String].contramap[Msg.Id](_.value)
  implicit private val lastMsgWrites: OWrites[Msg.Last]    = Json.writes[Msg.Last]
  implicit private val relationsWrites: OWrites[Relations] = Json.writes[Relations]

  def threads(me: User)(threads: List[MsgThread]): Fu[JsArray] =
    withContacts(me, threads) map { threads =>
      JsArray(threads map renderThread)
    }

  def convo(c: MsgConvo): JsObject = Json.obj(
    "thread"    -> renderThread(MsgThread.WithContact(c.thread, c.contact)),
    "msgs"      -> c.msgs.map(renderMsg),
    "relations" -> c.relations
  )

  def renderMsg(msg: Msg): JsObject = Json.obj(
    "id"   -> msg.id,
    "text" -> msg.text,
    "user" -> msg.user,
    "date" -> msg.date
  )

  def renderMsgWithThread(msg: Msg): JsObject =
    renderMsg(msg) + ("thread" -> threadIdWrites.writes(msg.thread))

  def searchResult(me: User)(res: MsgSearch.Result): Fu[JsObject] =
    withContacts(me, res.threads) map { threads =>
      Json.obj(
        "threads" -> threads.map(renderThread),
        "friends" -> res.friends,
        "users"   -> res.users
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
        "id"      -> t.thread.id,
        "contact" -> renderContact(t.contact)
      )
      .add("lastMsg" -> t.thread.lastMsg)

  private def renderContact(user: LightUser): JsObject =
    LightUser.lightUserWrites
      .writes(user)
      .add("online" -> isOnline(user.id))
}
