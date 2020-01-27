package lila.msg

import reactivemongo.api._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._
import org.joda.time.DateTime

import lila.common.{ Bus, LightUser }
import lila.db.dsl._
import lila.user.User
import lila.memo.RateLimit

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

  private val CreateLimitPerUser = new RateLimit[User.ID](
    credits = 20,
    duration = 24 hour,
    name = "PM creates per user",
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = new RateLimit[User.ID](
    credits = 20,
    duration = 1 minute,
    name = "PM replies per user",
    key = "msg_reply.user"
  )

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
      postable  <- canPost(me.id, userId)
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

  private[msg] def post(orig: User.ID, dest: User.ID, text: String): Funit = canPost(orig, dest) flatMap {
    _ ?? {
      val msg      = Msg.make(text, orig)
      val threadId = MsgThread.id(orig, dest)
      !colls.thread.exists($id(threadId)) flatMap { isNew =>
        val msgWrite = colls.msg.insert.one(writeMsg(msg, threadId))
        val threadWrite =
          if (isNew)
            colls.thread.insert.one(MsgThread.make(orig, dest, msg)).void
          else
            colls.thread.update
              .one(
                $id(threadId),
                $set("lastMsg" -> msg.asLast) ++ $unset("del")
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
      }
    }
  }

  private[msg] def canPost(orig: User.ID, dest: User.ID): Fu[Boolean] =
    !relationApi.fetchBlocks(dest, orig) >>& {
      canCreate(orig, dest) >>| canReply(orig, dest)
    }

  private def canCreate(orig: User.ID, dest: User.ID): Fu[Boolean] =
    prefApi.getPref(dest, _.message) flatMap {
      case lila.pref.Pref.Message.NEVER  => fuccess(false)
      case lila.pref.Pref.Message.FRIEND => relationApi.fetchFollows(dest, orig)
      case lila.pref.Pref.Message.ALWAYS => fuccess(true)
    }

  // Even if the dest prefs disallow it,
  // you can still reply if they recently messaged you,
  // unless they deleted the thread.
  private def canReply(orig: User.ID, dest: User.ID): Fu[Boolean] =
    colls.thread.exists(
      $id(MsgThread.id(orig, dest)) ++ $or(
        "del" $ne dest,
        $doc(
          "lastMsg.user" -> dest,
          "lastMsg.date" $gt DateTime.now.minusDays(3)
        )
      )
    )

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
