package lila.msg

import reactivemongo.api._
import play.api.data._
import play.api.data.Forms._
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
    notifier: MsgNotify
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def threadsOf(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find($doc("users" -> me.id))
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](50)

  def convoWith(me: User, username: String): Fu[MsgConvo] = {
    val userId = User.normalize(username)
    for {
      contact   <- lightUserApi async userId dmap (_ | LightUser.fallback(username))
      thread    <- threadOrNew(me.id, userId) flatMap readBy(me)
      msgs      <- threadMsgs(thread)
      relations <- relationApi.fetchRelations(me.id, userId)
    } yield MsgConvo(contact, thread, msgs, relations)
  }

  private def threadOrNew(user1: User.ID, user2: User.ID): Fu[MsgThread] =
    colls.thread.ext
      .find($id(MsgThread.id(user1, user2)))
      .one[MsgThread]
      .dmap { _ | MsgThread.make(user1, user2) }

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  private[msg] def post(orig: User.ID, dest: User.ID, text: String): Funit = {
    val msg = Msg.make(orig, dest, text)
    colls.msg.insert.one(msg) zip
      colls.thread.update.one(
        $id(MsgThread.id(orig, dest)),
        MsgThread.make(orig, dest).copy(lastMsg = msg.asLast.some),
        upsert = true
      ) >>- {
        notifier.onPost(msg.thread)
        Bus.publish(
          lila.hub.actorApi.socket.SendTos(
            Set(orig, dest),
            lila.socket.Socket.makeMessage("msgNew", json.renderMsgWithThread(msg))
          ),
          "socketUsers"
        )
      }
  }.void

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

  private def threadMsgs(thread: MsgThread): Fu[List[Msg]] =
    colls.msg.ext
      .find($doc("thread" -> thread.id))
      .sort($sort desc "date")
      .list[Msg](100)

  private def readBy(me: User)(thread: MsgThread): Fu[MsgThread] =
    if (thread.lastMsg.exists(_ unreadBy me.id))
      colls.thread.updateField($id(thread.id), "lastMsg.read", true) >>-
        notifier.onRead(thread.id) inject thread.setRead
    else
      fuccess(thread)
}
