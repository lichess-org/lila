package lila.msg

import play.api.data._
import play.api.data.Forms._
import reactivemongo.api._
import scala.concurrent.duration._

import lila.common.{ Bus, LightUser }
import lila.db.dsl._
import lila.user.User

final class MsgApi(
    colls: MsgColls,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi,
    json: MsgJson,
    notifier: MsgNotify,
    security: MsgSecurity
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def threadsOf(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find($doc("users" -> me.id, "del" $ne me.id))
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](50)

  def convoWith(me: User, username: String): Fu[MsgConvo] = {
    val userId = User.normalize(username)
    for {
      contact <- lightUserApi async userId dmap (_ | LightUser.fallback(username))
      threadId = MsgThread.id(me.id, userId)
      _         <- setReadBy(threadId, me)
      msgs      <- threadMsgsFor(threadId, me)
      relations <- relationApi.fetchRelations(me.id, userId)
      postable  <- security.may.post(me.id, userId)
    } yield MsgConvo(contact, msgs, relations, postable)
  }

  def delete(me: User, username: String): Funit = {
    val threadId = MsgThread.id(me.id, User.normalize(username))
    colls.msg.update
      .one($doc("tid" -> threadId), $addToSet("del" -> me.id), multi = true) >>
      colls.thread.update
        .one($id(threadId), $addToSet("del" -> me.id))
        .void
  }

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  private[msg] def post(orig: User.ID, dest: User.ID, text: String): Funit = {
    val msg      = Msg.make(text, orig)
    val threadId = MsgThread.id(orig, dest)
    !colls.thread.exists($id(threadId)) flatMap { isNew =>
      security.can.post(dest, msg, isNew) flatMap {
        case _: MsgSecurity.Reject => funit
        case send: MsgSecurity.Send =>
          val msgWrite = colls.msg.insert.one(writeMsg(msg, threadId))
          val threadWrite =
            if (isNew)
              colls.thread.insert.one {
                writeThread(MsgThread.make(orig, dest, msg), delBy = send.mute option dest)
              }.void
            else
              colls.thread.update
                .one(
                  $id(threadId),
                  $set("lastMsg" -> msg.asLast) ++ $pull(
                    // unset deleted by receiver unless the message is muted
                    "del" $in (orig :: (!send.mute).option(dest).toList)
                  )
                )
                .void
          (msgWrite zip threadWrite).void >>- {
            notifier.onPost(threadId)
            Bus.publish(
              lila.hub.actorApi.socket.SendTo(
                dest,
                lila.socket.Socket.makeMessage("msgNew", json.renderMsg(msg))
              ),
              "socketUsers"
            )
          }
        case _ => funit
      }
    }
  }

  def setRead(userId: User.ID, contactId: User.ID): Funit = {
    val threadId = MsgThread.id(userId, contactId)
    colls.thread
      .updateField(
        $id(threadId) ++ $doc("lastMsg.user" -> contactId),
        "lastMsg.read",
        true
      )
      .void >>- notifier.onRead(threadId)
  }

  private val unreadCountCache = cacheApi[User.ID, Int](256, "message.unreadCount") {
    _.expireAfterWrite(10 seconds)
      .buildAsyncFuture[User.ID, Int] { userId =>
        colls.thread.countSel($doc("users" -> userId, "lastMsg.read" -> false, "lastMsg.user" $ne userId))
      }
  }

  def unreadCount(me: User): Fu[Int] = unreadCountCache.get(me.id)

  private val msgProjection = $doc("_id" -> false, "tid" -> false)

  private def threadMsgsFor(threadId: MsgThread.Id, me: User): Fu[List[Msg]] =
    colls.msg.ext
      .find(
        $doc("tid" -> threadId, "del" $ne me.id),
        msgProjection
      )
      .sort($sort desc "date")
      .list[Msg](100)

  private def setReadBy(threadId: MsgThread.Id, me: User): Funit =
    colls.thread.updateField(
      $id(threadId) ++ $doc(
        "lastMsg.user" $ne me.id,
        "lastMsg.read" -> false
      ),
      "lastMsg.read",
      true
    ) map { res =>
      if (res.nModified > 0) notifier.onRead(threadId)
    }
}
