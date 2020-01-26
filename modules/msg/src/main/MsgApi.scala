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
    json: MsgJson,
    notifier: MsgNotify
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def threads(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find(
        $doc(
          "users" -> me.id,
          "blockers" $ne me.id
        )
      )
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](100)

  def convoWith(me: User, other: LightUser): Fu[MsgThread.WithMsgs] =
    threadOrNew(me.id, other.id)
      .flatMap(readBy(me))
      .flatMap(withMsgs)

  def threadOrNew(user1: User.ID, user2: User.ID): Fu[MsgThread] =
    colls.thread.ext
      .find($id(MsgThread.id(user1, user2)))
      .one[MsgThread]
      .dmap { _ | MsgThread.make(user1, user2) }

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  def post(orig: User.ID, dest: User.ID, text: String): Funit = {
    val msg = Msg.make(orig, dest, text)
    colls.msg.insert.one(msg) zip
      colls.thread.update.one(
        $id(MsgThread.id(orig, dest)),
        MsgThread.make(orig, dest).copy(lastMsg = msg.asLast.some),
        upsert = true
      ) >>- {
        notifier.onPost(msg)
        Bus.publish(
          lila.hub.actorApi.socket.SendTos(
            Set(orig, dest),
            lila.socket.Socket.makeMessage("msgNew", json.renderMsgWithThread(msg))
          ),
          "socketUsers"
        )
      }
  }.void

  def setRead(userId: User.ID, contactId: User.ID): Funit =
    colls.thread
      .updateField(
        $id(MsgThread.id(userId, contactId)) ++ $doc("lastMsg.user" -> contactId),
        "lastMsg.read",
        true
      )
      .void

  private val unreadCountCache = cacheApi[User.ID, Int](256, "message.unreadCount") {
    _.expireAfterWrite(10 seconds)
      .buildAsyncFuture[User.ID, Int] { userId =>
        colls.thread.countSel($doc("users" -> userId, "lastMsg.read" -> false, "lastMsg.user" $ne userId))
      }
  }

  def unreadCount(me: User): Fu[Int] = unreadCountCache.get(me.id)

  private def withMsgs(thread: MsgThread): Fu[MsgThread.WithMsgs] =
    colls.msg.ext
      .find($doc("thread" -> thread.id))
      .sort($sort desc "date")
      .list[Msg](100)
      .map { MsgThread.WithMsgs(thread, _) }

  private def readBy(me: User)(thread: MsgThread): Fu[MsgThread] =
    if (thread.lastMsg.exists(_ unreadBy me.id))
      colls.thread.updateField($id(thread.id), "lastMsg.read", true) inject thread.setRead
    else
      fuccess(thread)
}
