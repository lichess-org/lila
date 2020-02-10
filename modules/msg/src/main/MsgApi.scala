package lila.msg

import reactivemongo.api._
import scala.concurrent.duration._
import org.joda.time.DateTime
import scala.util.Try

import lila.common.{ Bus, LightUser }
import lila.common.config.MaxPerPage
import lila.db.dsl._
import lila.user.User

final class MsgApi(
    colls: MsgColls,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi,
    json: MsgJson,
    notifier: MsgNotify,
    security: MsgSecurity,
    shutup: lila.hub.actors.Shutup
)(implicit ec: scala.concurrent.ExecutionContext) {

  val msgsPerPage = MaxPerPage(100)

  import BsonHandlers._

  def threadsOf(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find($doc("users" -> me.id, "del" $ne me.id))
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](50)

  def convoWith(me: User, username: String, beforeMillis: Option[Long] = None): Fu[Option[MsgConvo]] = {
    val userId   = User.normalize(username)
    val threadId = MsgThread.id(me.id, userId)
    val before = beforeMillis flatMap { millis =>
      Try(new DateTime(millis)).toOption
    }
    (userId != me.id) ?? lightUserApi.async(userId).flatMap {
      _ ?? { contact =>
        for {
          _         <- setReadBy(threadId, me)
          msgs      <- threadMsgsFor(threadId, me, before)
          relations <- relationApi.fetchRelations(me.id, userId)
          postable  <- security.may.post(me.id, userId, isNew = msgs.headOption.isEmpty)
        } yield MsgConvo(contact, msgs, relations, postable).some
      }
    }
  }

  def delete(me: User, username: String): Funit = {
    val threadId = MsgThread.id(me.id, User.normalize(username))
    colls.msg.update
      .one($doc("tid" -> threadId), $addToSet("del" -> me.id), multi = true) >>
      colls.thread.update
        .one($id(threadId), $addToSet("del" -> me.id))
        .void
  }

  def post(
      orig: User.ID,
      dest: User.ID,
      text: String,
      unlimited: Boolean = false
  ): Funit = Msg.make(text, orig) ?? { msg =>
    val threadId = MsgThread.id(orig, dest)
    for {
      contacts <- userRepo.contacts(orig, dest) orFail "Missing convo contact user"
      isNew    <- !colls.thread.exists($id(threadId))
      verdict  <- security.can.post(contacts, msg.text, isNew, unlimited)
      res <- verdict match {
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
                    // unset "deleted by receiver" unless the message is muted
                    "del" $in (orig :: (!send.mute).option(dest).toList)
                  )
                )
                .void
          (msgWrite zip threadWrite).void >>- {
            if (!send.mute) {
              notifier.onPost(threadId)
              Bus.publish(
                lila.hub.actorApi.socket.SendTo(
                  dest,
                  lila.socket.Socket.makeMessage("msgNew", json.renderMsg(msg))
                ),
                "socketUsers"
              )
              shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(orig, dest, text)
            }
          }
        case _ => funit
      }
    } yield res
  }

  def setRead(userId: User.ID, contactId: User.ID): Funit = {
    val threadId = MsgThread.id(userId, contactId)
    colls.thread
      .updateField(
        $id(threadId) ++ $doc("lastMsg.user" -> contactId),
        "lastMsg.read",
        true
      )
      .map { res =>
        if (res.nModified > 0) notifier.onRead(threadId)
      }
  }

  def postPreset(dest: User, preset: MsgPreset): Funit =
    systemPost(dest.id, preset.text)

  def systemPost(destId: User.ID, text: String) =
    post(User.lichessId, destId, text, unlimited = true)

  def multiPost(orig: User, dests: List[User], text: String): Funit =
    dests
      .map { dest =>
        post(orig.id, dest.id, text, unlimited = true)
      }
      .sequenceFu
      .void

  def recentByForMod(user: User, nb: Int): Fu[List[MsgConvo]] =
    colls.thread.ext
      .find($doc("users" -> user.id))
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](nb)
      .flatMap {
        _.map { thread =>
          colls.msg.ext
            .find($doc("tid" -> thread.id), msgProjection)
            .sort($sort desc "date")
            .list[Msg](10)
            .flatMap { msgs =>
              lightUserApi async thread.other(user) map { contact =>
                MsgConvo(
                  contact | LightUser.fallback(thread other user),
                  msgs,
                  lila.relation.Relations(none, none),
                  false
                )
              }
            }
        }.sequenceFu
      }

  def deleteAllBy(user: User): Funit =
    colls.thread.list[MsgThread]($doc("users" -> user.id)) flatMap { threads =>
      colls.thread.delete.one($doc("users" -> user.id)) >>
        colls.msg.delete.one($doc("tid" $in threads.map(_.id))) >>
        notifier.deleteAllBy(threads, user)
    }

  def unreadCount(me: User): Fu[Int] = unreadCountCache.get(me.id)

  private val unreadCountCache = cacheApi[User.ID, Int](256, "message.unreadCount") {
    _.expireAfterWrite(10 seconds)
      .buildAsyncFuture[User.ID, Int] { userId =>
        colls.thread.countSel($doc("users" -> userId, "lastMsg.read" -> false, "lastMsg.user" $ne userId))
      }
  }

  private val msgProjection = $doc("_id" -> false, "tid" -> false)

  private def threadMsgsFor(threadId: MsgThread.Id, me: User, before: Option[DateTime]): Fu[List[Msg]] =
    colls.msg.ext
      .find(
        $doc("tid" -> threadId, "del" $ne me.id) ++ before.?? { b =>
          $doc("date" $lt b)
        },
        msgProjection
      )
      .sort($sort desc "date")
      .list[Msg](msgsPerPage.value)

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
