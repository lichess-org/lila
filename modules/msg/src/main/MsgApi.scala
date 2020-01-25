package lila.msg

import reactivemongo.api._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

final class MsgApi(
    colls: MsgColls,
    cacheApi: lila.memo.CacheApi
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

  def convoWith(me: User, other: User): Fu[MsgThread.WithMsgs] =
    colls.thread.ext
      .find(
        $doc("_id" -> MsgThread.id(me.id, other.id))
      )
      .one[MsgThread]
      .dmap { _ | MsgThread.make(me.id, other.id) }
      .flatMap(readBy(me))
      .flatMap(withMsgs)

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  def post(me: User, other: User, text: String): Fu[Msg] = {
    val msg = Msg.make(me.id, other.id, text)
    colls.msg.insert.one(msg) zip
      colls.thread.update.one(
        $id(msg.thread),
        MsgThread.make(me.id, other.id).copy(lastMsg = msg.asLast.some),
        upsert = true
      ) inject msg
  }

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
