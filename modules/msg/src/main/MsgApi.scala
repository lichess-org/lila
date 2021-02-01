package lila.msg

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.util.Try

import lila.common.config.MaxPerPage
import lila.common.LilaStream
import lila.common.{ Bus, LightUser }
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class MsgApi(
    colls: MsgColls,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi,
    json: MsgJson,
    notifier: MsgNotify,
    security: MsgSecurity,
    shutup: lila.hub.actors.Shutup,
    spam: lila.security.Spam
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  val msgsPerPage = MaxPerPage(100)

  import BsonHandlers._
  import MsgApi._

  def threadsOf(me: User): Fu[List[MsgThread]] =
    colls.thread
      .find($doc("users" -> me.id, "del" $ne me.id))
      .sort($sort desc "lastMsg.date")
      .cursor[MsgThread]()
      .list(50)
      .map(prioritize)

  private def prioritize(threads: List[MsgThread]) =
    threads.find(_.isPriority) match {
      case None        => threads
      case Some(found) => found :: threads.filterNot(_.isPriority)
    }

  def convoWith(me: User, username: String, beforeMillis: Option[Long] = None): Fu[Option[MsgConvo]] = {
    val userId   = User.normalize(username)
    val threadId = MsgThread.id(me.id, userId)
    val before = beforeMillis flatMap { millis =>
      Try(new DateTime(millis)).toOption
    }
    (userId != me.id) ?? lightUserApi.async(userId).flatMap {
      _ ?? { contact =>
        for {
          _         <- setReadBy(threadId, me, userId)
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
      multi: Boolean = false
  ): Fu[PostResult] =
    Msg.make(text, orig).fold[Fu[PostResult]](fuccess(PostResult.Invalid)) { msgPre =>
      val threadId = MsgThread.id(orig, dest)
      for {
        contacts <- userRepo.contacts(orig, dest) orFail s"Missing convo contact user $orig->$dest"
        isNew    <- !colls.thread.exists($id(threadId))
        verdict  <- security.can.post(contacts, msgPre.text, isNew, unlimited = multi)
        res <- verdict match {
          case MsgSecurity.Limit     => fuccess(PostResult.Limited)
          case _: MsgSecurity.Reject => fuccess(PostResult.Bounced)
          case send: MsgSecurity.Send =>
            val msg =
              if (verdict == MsgSecurity.Spam) msgPre.copy(text = spam.replace(msgPre.text)) else msgPre
            val msgWrite = colls.msg.insert.one(writeMsg(msg, threadId))
            val threadWrite =
              if (isNew)
                colls.thread.insert.one {
                  writeThread(
                    MsgThread.make(orig, dest, msg),
                    delBy = List(
                      multi option orig,
                      send.mute option dest
                    ).flatten
                  )
                }.void
              else
                colls.thread.update
                  .one(
                    $id(threadId),
                    $set("lastMsg" -> msg.asLast) ++ {
                      if (multi) $pull("del" -> List(orig))
                      else
                        $pull(
                          // unset "deleted by receiver" unless the message is muted
                          "del" $in (orig :: (!send.mute).option(dest).toList)
                        )
                    }
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
            } inject PostResult.Success
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
      .flatMap { res =>
        (res.nModified > 0) ?? notifier.onRead(threadId, userId, contactId)
      }
  }

  def postPreset(dest: User, preset: MsgPreset): Fu[PostResult] =
    systemPost(dest.id, preset.text)

  def systemPost(destId: User.ID, text: String) =
    post(User.lichessId, destId, text, multi = true)

  def multiPost(orig: User, destSource: Source[User.ID, _], text: String): Fu[Int] =
    destSource
      .filter(orig.id !=)
      .mapAsync(4) {
        post(orig.id, _, text, multi = true).logFailure(logger).nevermind(PostResult.Invalid)
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()

  def cliMultiPost(orig: String, dests: Seq[User.ID], text: String): Fu[String] =
    userRepo named orig flatMap {
      case None         => fuccess(s"Unknown sender $orig")
      case Some(sender) => multiPost(sender, Source(dests), text) inject "done"
    }

  def recentByForMod(user: User, nb: Int): Fu[List[MsgConvo]] =
    colls.thread
      .find($doc("users" -> user.id))
      .sort($sort desc "lastMsg.date")
      .cursor[MsgThread]()
      .list(nb)
      .flatMap {
        _.map { thread =>
          colls.msg
            .find($doc("tid" -> thread.id), msgProjection)
            .sort($sort desc "date")
            .cursor[Msg]()
            .list(10)
            .flatMap { msgs =>
              lightUserApi async thread.other(user) map { contact =>
                MsgConvo(
                  contact | LightUser.fallback(thread other user),
                  msgs,
                  lila.relation.Relations(none, none),
                  postable = false
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

  private val msgProjection = $doc("_id" -> false, "tid" -> false).some

  private def threadMsgsFor(threadId: MsgThread.Id, me: User, before: Option[DateTime]): Fu[List[Msg]] =
    colls.msg
      .find(
        $doc("tid" -> threadId, "del" $ne me.id) ++ before.?? { b =>
          $doc("date" $lt b)
        },
        msgProjection
      )
      .sort($sort desc "date")
      .cursor[Msg]()
      .list(msgsPerPage.value)

  private def setReadBy(threadId: MsgThread.Id, me: User, contactId: User.ID): Funit =
    colls.thread.updateField(
      $id(threadId) ++ $doc(
        "lastMsg.user" $ne me.id,
        "lastMsg.read" -> false
      ),
      "lastMsg.read",
      true
    ) flatMap { res =>
      (res.nModified > 0) ?? notifier.onRead(threadId, me.id, contactId)
    }

  def allMessagesOf(userId: User.ID): Fu[Vector[(User.ID, String, DateTime)]] =
    colls.thread
      .aggregateWith[Bdoc](
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        List(
          Match($doc("users" -> userId)),
          Project($id(true)),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> colls.msg.name,
                "let"  -> $doc("t" -> "$_id"),
                "pipeline" -> $arr(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc(
                        "$and" -> $arr(
                          $doc("$eq" -> $arr("$user", userId)),
                          $doc("$eq" -> $arr("$tid", "$$t"))
                        )
                      )
                    )
                  )
                ),
                "as" -> "msg"
              )
            )
          ),
          Unwind("msg"),
          Project($doc("msg.text" -> true, "msg.date" -> true))
        )
      }
      .collect[Vector](maxDocs = 9000)
      .map { docs =>
        for {
          doc  <- docs
          id   <- doc string "_id"
          dest <- id.split(MsgThread.idSep).find(userId !=)
          msg  <- doc child "msg"
          text <- msg string "text"
          date <- msg.getAsOpt[DateTime]("date")
        } yield (dest, text, date)
      }
}

object MsgApi {

  sealed trait PostResult

  object PostResult {
    case object Success extends PostResult
    case object Invalid extends PostResult
    case object Limited extends PostResult
    case object Bounced extends PostResult
  }
}
