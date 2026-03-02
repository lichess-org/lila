package lila.msg

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser
import lila.core.relation.Relations

final class MsgJson(
    lightUserApi: lila.core.user.LightUserApi,
    isOnline: lila.core.socket.IsOnline
)(using Executor):

  private given lastMsgWrites: OWrites[Msg.Last] = Json.writes
  private given relationsWrites: OWrites[Relations] = Json.writes
  private given modDetailsWrites: OWrites[ContactDetailsForMods] = Json.writes

  def threadsJson(threads: List[MsgThread])(using me: Me): Fu[JsArray] =
    withContacts(threads).map: threads =>
      JsArray(threads.map(renderThread))

  def contacts(threads: List[MsgThread])(using me: Me): Fu[JsObject] =
    threadsJson(threads).map: threads =>
      Json.obj("contacts" -> threads)

  def convo(c: MsgConvo): JsObject =
    Json
      .obj(
        "user" -> renderContact(c.contact),
        "msgs" -> c.msgs.map(renderMsg),
        "relations" -> c.relations,
        "postable" -> c.postable
      )
      .add("modDetails" -> c.contactDetailsForMods)

  def renderMsg(msg: Msg): JsObject =
    Json.obj(
      "text" -> msg.text,
      "user" -> msg.user,
      "date" -> msg.date
    )

  def searchResult(res: MsgSearch.Result)(using me: Me): Fu[JsObject] =
    withContacts(res.threads).map: threads =>
      Json.obj(
        "contacts" -> threads.map(renderThread),
        "friends" -> res.friends,
        "users" -> res.users
      )

  private def withContacts(threads: List[MsgThread])(using me: Me): Fu[List[MsgThread.WithContact]] =
    lightUserApi
      .asyncMany(threads.map(_.other))
      .map: users =>
        threads
          .zip(users)
          .map: (thread, userOption) =>
            MsgThread.WithContact(thread, userOption | LightUser.fallback(thread.other.into(UserName)))

  private def renderThread(t: MsgThread.WithContact)(using me: Option[Me]) =
    Json.obj(
      "user" -> renderContact(t.contact),
      "lastMsg" -> me.fold(t.thread.lastMsg): me =>
        if t.thread.maskFor.contains(me) then t.thread.maskWith.getOrElse(t.thread.lastMsg)
        else t.thread.lastMsg
    )

  private def renderContact(user: LightUser): JsObject =
    lila.common.Json.lightUser
      .writeNoId(user)
      .add("online" -> isOnline.exec(user.id))
