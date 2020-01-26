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
    prefApi: lila.pref.PrefApi,
    json: MsgJson,
    notifier: MsgNotify
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
      contact   <- lightUserApi async userId dmap (_ | LightUser.fallback(username))
      thread    <- threadOrNew(me.id, userId) flatMap readBy(me)
      msgs      <- threadMsgsFor(thread, me)
      relations <- relationApi.fetchRelations(me.id, userId)
      postable  <- canPost(me.id, userId)
    } yield MsgConvo(contact, thread, msgs, relations, postable)
  }

  def delete(me: User, username: String): Funit = {
    val threadId = MsgThread.id(me.id, User.normalize(username))
    colls.msg.update
      .one($doc("thread" -> threadId), $addToSet("del" -> me.id), multi = true) >>
      colls.thread.update
        .one($id(threadId), $addToSet("del" -> me.id))
        .void
  }

  private def threadOrNew(user1: User.ID, user2: User.ID): Fu[MsgThread] =
    colls.thread.ext
      .find($id(MsgThread.id(user1, user2)))
      .one[MsgThread]
      .dmap { _ | MsgThread.make(user1, user2) }

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  private[msg] def post(orig: User.ID, dest: User.ID, text: String): Funit = canPost(orig, dest) flatMap {
    _ ?? {
      val msg = Msg.make(orig, dest, text)
      colls.msg.insert.one(msg) zip
        colls.thread.update.one(
          $id(MsgThread.id(orig, dest)),
          $set(threadBSONHandler.write(MsgThread.make(orig, dest).copy(lastMsg = msg.asLast.some))) ++
            $unset("del"),
          upsert = true
        ) >>- {
          notifier.onPost(msg.thread)
          Bus.publish(
            lila.hub.actorApi.socket.SendTo(
              dest,
              lila.socket.Socket.makeMessage("msgNew", json.renderMsg(msg))
            ),
            "socketUsers"
          )
        }
    }.void
  }

  private[msg] def canPost(orig: User.ID, dest: User.ID): Fu[Boolean] =
    prefApi.getPref(dest, _.message) flatMap {
      case lila.pref.Pref.Message.NEVER  => fuccess(false)
      case lila.pref.Pref.Message.FRIEND => relationApi.fetchFollows(dest, orig)
      case lila.pref.Pref.Message.ALWAYS => !relationApi.fetchBlocks(dest, orig)
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

  private def threadMsgsFor(thread: MsgThread, me: User): Fu[List[Msg]] =
    colls.msg.ext
      .find($doc("thread" -> thread.id, "del" $ne me.id))
      .sort($sort desc "date")
      .list[Msg](100)

  private def readBy(me: User)(thread: MsgThread): Fu[MsgThread] =
    if (thread.lastMsg.exists(_ unreadBy me.id))
      colls.thread.updateField($id(thread.id), "lastMsg.read", true) >>-
        notifier.onRead(thread.id) inject thread.setRead
    else
      fuccess(thread)
}
