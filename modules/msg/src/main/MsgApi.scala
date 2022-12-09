package lila.msg

import akka.stream.scaladsl.*
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference
import scala.util.Try

import lila.common.config.MaxPerPage
import lila.common.{ Bus, LightUser, LilaStream }
import lila.db.dsl.{ *, given }
import lila.relation.Relations
import lila.user.Holder
import lila.user.{ User, UserRepo }

final class MsgApi(
    colls: MsgColls,
    userRepo: UserRepo,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi,
    json: MsgJson,
    notifier: MsgNotify,
    security: MsgSecurity,
    shutup: lila.hub.actors.Shutup,
    spam: lila.security.Spam
)(using scala.concurrent.ExecutionContext, akka.stream.Materializer):

  val msgsPerPage = MaxPerPage(100)

  import BsonHandlers.{ *, given }
  import MsgApi.*

  def threadsOf(me: User): Fu[List[MsgThread]] =
    colls.thread
      .find($doc("users" -> me.id, "del" $ne me.id))
      .sort($sort desc "lastMsg.date")
      .cursor[MsgThread]()
      .list(50)
      .map(prioritize)

  private def prioritize(threads: List[MsgThread]) =
    threads.find(_.isPriority) match
      case None        => threads
      case Some(found) => found :: threads.filterNot(_.isPriority)

  def convoWith(me: User, username: UserStr, beforeMillis: Option[Long] = None): Fu[Option[MsgConvo]] =
    val userId   = username.id
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

  def delete(me: User, username: UserStr): Funit =
    val threadId = MsgThread.id(me.id, username.id)
    colls.msg.update
      .one($doc("tid" -> threadId), $addToSet("del" -> me.id), multi = true) >>
      colls.thread.update
        .one($id(threadId), $addToSet("del" -> me.id))
        .void

  def post(
      orig: UserId,
      dest: UserId,
      text: String,
      multi: Boolean = false,
      ignoreSecurity: Boolean = false
  ): Fu[PostResult] =
    Msg.make(text, orig).fold[Fu[PostResult]](fuccess(PostResult.Invalid)) { msgPre =>
      val threadId = MsgThread.id(orig, dest)
      for {
        contacts <- userRepo.contacts(orig, dest) orFail s"Missing convo contact user $orig->$dest"
        isNew    <- !colls.thread.exists($id(threadId))
        verdict <-
          if (ignoreSecurity) fuccess(MsgSecurity.Ok)
          else security.can.post(contacts, msgPre.text, isNew, unlimited = multi)
        _ = lila.mon.msg.post(verdict.toString, isNew = isNew, multi = multi).increment()
        res <- verdict match
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
              if (!send.mute)
                notifier.onPost(threadId)
                Bus.publish(
                  lila.hub.actorApi.socket.SendTo(
                    dest,
                    lila.socket.Socket.makeMessage("msgNew", json.renderMsg(msg))
                  ),
                  "socketUsers"
                )
                shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(orig, dest, text)
            } inject PostResult.Success
      } yield res
    }

  def setRead(userId: UserId, contactId: UserId): Funit =
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

  def postPreset(destId: UserId, preset: MsgPreset): Fu[PostResult] =
    systemPost(destId, preset.text)

  def systemPost(destId: UserId, text: String) =
    post(User.lichessId, destId, text, multi = true, ignoreSecurity = true)

  def multiPost(orig: Holder, destSource: Source[UserId, ?], text: String): Fu[Int] =
    destSource
      .filter(orig.id !=)
      .mapAsync(4) {
        post(orig.id, _, text, multi = true).logFailure(logger).recoverDefault(PostResult.Invalid)
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()

  def cliMultiPost(orig: UserStr, dests: Seq[UserId], text: String): Fu[String] =
    userRepo byId orig flatMap {
      case None         => fuccess(s"Unknown sender $orig")
      case Some(sender) => multiPost(Holder(sender), Source(dests), text) inject "done"
    }

  def recentByForMod(user: User, nb: Int): Fu[List[ModMsgConvo]] =
    colls.thread
      .aggregateList(nb) { framework =>
        import framework.*
        Match($doc("users" -> user.id)) -> List(
          Sort(Descending("lastMsg.date")),
          Limit(nb),
          UnwindField("users"),
          Match($doc("users" $ne user.id)),
          PipelineOperator(
            $lookup.pipeline(
              from = colls.msg,
              as = "msgs",
              local = "_id",
              foreign = "tid",
              pipe = List(
                $doc("$sort"    -> $sort.desc("date")),
                $doc("$limit"   -> 11),
                $doc("$project" -> msgProjection)
              )
            )
          ),
          PipelineOperator(
            $lookup.simple(
              from = userRepo.coll,
              as = "contact",
              local = "users",
              foreign = "_id"
            )
          ),
          UnwindField("contact")
        )
      } flatMap { docs =>
      (for {
        doc     <- docs
        msgs    <- doc.getAsOpt[List[Msg]]("msgs")
        contact <- doc.getAsOpt[User]("contact")
      } yield relationApi.fetchRelation(contact.id, user.id) map { relation =>
        ModMsgConvo(contact, msgs take 10, Relations(relation, none), msgs.length == 11)
      }).sequenceFu
    }

  def deleteAllBy(user: User): Funit =
    colls.thread.list[MsgThread]($doc("users" -> user.id)) flatMap { threads =>
      colls.thread.delete.one($doc("users" -> user.id)) >>
        colls.msg.delete.one($doc("tid" $in threads.map(_.id))) >>
        notifier.deleteAllBy(threads, user)
    }

  private val msgProjection = $doc("_id" -> false, "tid" -> false)

  private def threadMsgsFor(threadId: MsgThread.Id, me: User, before: Option[DateTime]): Fu[List[Msg]] =
    colls.msg
      .find(
        $doc("tid" -> threadId) ++ before.?? { b =>
          $doc("date" $lt b)
        },
        msgProjection.some
      )
      .sort($sort desc "date")
      .cursor[Bdoc]()
      .list(msgsPerPage.value)
      .map {
        _.flatMap { doc =>
          doc.getAsOpt[List[UserId]]("del").fold(true)(!_.has(me.id)) ?? doc.asOpt[Msg]
        }
      }

  private def setReadBy(threadId: MsgThread.Id, me: User, contactId: UserId): Funit =
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

  def hasUnreadLichessMessage(userId: UserId): Fu[Boolean] = colls.thread.secondaryPreferred.exists(
    $id(MsgThread.id(userId, User.lichessId)) ++ $doc("lastMsg.read" -> false)
  )

  def allMessagesOf(userId: UserId): Source[(String, DateTime), ?] =
    colls.thread
      .aggregateWith[Bdoc](
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework.*
        List(
          Match($doc("users" -> userId)),
          Project($id(true)),
          PipelineOperator(
            $lookup.pipelineFull(
              from = colls.msg.name,
              as = "msg",
              let = $doc("t" -> "$_id"),
              pipe = List(
                $doc(
                  "$match" ->
                    $expr(
                      $and(
                        $doc("$eq" -> $arr("$user", userId)),
                        $doc("$eq" -> $arr("$tid", "$$t")),
                        $doc(
                          "$not" -> $doc(
                            "$regexMatch" -> $doc(
                              "input" -> "$text",
                              "regex" -> "You received this because you are (subscribed to messages|part) of the team"
                            )
                          )
                        )
                      )
                    )
                )
              )
            )
          ),
          Unwind("msg"),
          Project($doc("_id" -> false, "msg.text" -> true, "msg.date" -> true))
        )
      }
      .documentSource()
      .mapConcat { doc =>
        (for {
          msg  <- doc child "msg"
          text <- msg string "text"
          date <- msg.getAsOpt[DateTime]("date")
        } yield (text, date)).toList
      }

object MsgApi:
  enum PostResult:
    case Success, Invalid, Limited, Bounced
