package lila.msg

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.common.{ Bus, LilaStream }
import lila.core.msg.{ PostResult, IdText }
import lila.core.relation.Relations
import lila.db.dsl.{ *, given }
import lila.core.perm.Granter

final class MsgApi(
    colls: MsgColls,
    contactApi: ContactApi,
    userApi: lila.core.user.UserApi,
    userRepo: lila.core.user.UserRepo,
    prefApi: lila.core.pref.PrefApi,
    lightUserApi: lila.core.user.LightUserApi,
    relationApi: lila.core.relation.RelationApi,
    json: MsgJson,
    notifier: MsgNotify,
    security: MsgSecurity,
    shutupApi: lila.core.shutup.ShutupApi,
    spam: lila.core.security.SpamApi
)(using Executor, akka.stream.Materializer)
    extends lila.core.msg.MsgApi:

  val msgsPerPage = MaxPerPage(100)
  val inboxSize = 50

  import BsonHandlers.{ *, given }

  def myThreads(using me: Me): Fu[List[MsgThread]] =
    colls.thread
      .find(selectMyThreads)
      .sort($sort.desc("lastMsg.date"))
      .cursor[MsgThread]()
      .list(inboxSize)
      .flatMap(maybeSortAgain)
      .map(prioritize)

  // maybeSortAgain maintains usable inbox thread ordering for team leaders after PM alls.
  private def maybeSortAgain(threads: List[MsgThread])(using me: Me): Fu[List[MsgThread]] =
    val candidates = threads.filter(_.maskFor.has(me.userId))
    if candidates.isEmpty then
      // we're done
      fuccess(threads)
    else
      val receivedMultis = threads.filter(_.maskFor.exists(_.isnt(me)))
      colls.thread
        .find(selectMyThreads)
        .sort($sort.desc("maskWith.date")) // sorting on maskWith.date now
        .cursor[MsgThread]()
        .list(inboxSize)
        // last we filter receivedMultis and reinsert them according to their lastMsg.date
        .map(sorted => merge(sorted.filterNot(receivedMultis.contains), receivedMultis))

  private def selectMyThreads(using me: Me) = $doc("users" -> me.userId) ++ selectNotDeleted

  private def merge(sorteds: List[MsgThread], multis: List[MsgThread]): List[MsgThread] =
    (sorteds, multis) match
      case (Nil, Nil) => Nil
      case (_, Nil) => sorteds
      case (Nil, _) => multis
      case (sorted :: sortedTail, multi :: multiTail) =>
        // we're comparing lastMsg.date in multis to maskWith.date in sorteds
        if sorted.maskWith.exists(sortMsg => multi.lastMsg.date.isAfter(sortMsg.date))
        then multi :: merge(sorteds, multiTail)
        else sorted :: merge(sortedTail, multis)

  private def prioritize(threads: List[MsgThread]) =
    threads
      .find(_.isPriority)
      .fold(threads): found =>
        found :: threads.filterNot(_.isPriority)

  def convoWithMe(username: UserStr, beforeMillis: Option[Long] = None)(using me: Me): Fu[Option[MsgConvo]] =
    val userId = username.id
    val threadId = MsgThread.id(me, userId)
    val before = beforeMillis.flatMap: millis =>
      util.Try(millisToInstant(millis)).toOption
    userId
      .isnt(me)
      .so:
        lightUserApi.async(userId).flatMapz { contact =>
          for
            _ <- setReadBy(threadId, me, userId)
            msgs <- threadMsgsFor(threadId, me, before)
            relations <- relationApi.fetchRelations(me, userId)
            postable <- security.may.post(me, userId, isNew = msgs.headOption.isEmpty)
            details <- Granter(_.PublicMod).optionFu(fetchContactDetailsForMods(userId))
          yield MsgConvo(contact, msgs, relations, postable, details).some
        }

  private def fetchContactDetailsForMods(userId: UserId): Fu[ContactDetailsForMods] = for
    kid <- userApi.isKid(userId)
    pref <- prefApi.getMessage(userId)
  yield ContactDetailsForMods(kid, pref == lila.core.pref.Message.ALWAYS)

  def delete(username: UserStr)(using me: Me): Funit =
    val threadId = MsgThread.id(me, username.id)
    colls.msg.update
      .one($doc("tid" -> threadId), $addToSet("del" -> me.userId), multi = true) >>
      colls.thread.update
        .one($id(threadId), $addToSet("del" -> me.userId))
        .void

  def post(
      orig: UserId,
      dest: UserId,
      text: String,
      multi: Boolean = false,
      date: Instant = nowInstant,
      ignoreSecurity: Boolean = false
  ): Fu[PostResult] =
    orig.isnt(dest).so(Msg.make(text, orig, date)).fold(fuccess(PostResult.Invalid)) { msgPre =>
      val threadId = MsgThread.id(orig, dest)
      for
        contacts <- contactApi.contacts(orig, dest).orFail(s"Missing convo contact user $orig->$dest")
        isNew <- colls.thread.exists($id(threadId)).not
        verdict <-
          if ignoreSecurity then fuccess(MsgSecurity.Ok)
          else security.can.post(contacts, msgPre.text, isNew, unlimited = multi)
        _ = lila.mon.msg.post(verdict.toString, isNew = isNew, multi = multi).increment()
        maskFor = multi.option(orig)
        maskWith <-
          if multi && !isNew then lastDirectMsg(threadId, orig) else fuccess(None)
        res <- verdict match
          case MsgSecurity.Limit => fuccess(PostResult.Limited)
          case _: MsgSecurity.Reject => fuccess(PostResult.Bounced)
          case send: MsgSecurity.Send =>
            val msg =
              if verdict == MsgSecurity.Spam
              then
                logger.branch("spam").warn(s"$orig -> $dest $msgPre.text")
                msgPre.copy(text = spam.replace(msgPre.text))
              else msgPre
            val msgWrite = colls.msg.insert.one(writeMsg(msg, threadId))
            def threadWrite =
              if isNew then
                colls.thread.insert.one:
                  writeThread(
                    MsgThread.make(orig, dest, msg, maskFor, maskWith),
                    List(multi.option(orig), send.mute.option(dest)).flatten
                  )
              else
                colls.thread.update
                  .one(
                    $id(threadId),
                    if multi
                    then
                      $set("lastMsg" -> msg.asLast, "maskFor" -> maskFor, "maskWith" -> maskWith)
                        ++ $pull("del" -> List(orig))
                    else
                      $set("lastMsg" -> msg.asLast, "maskWith.date" -> msg.date)
                        ++ $unset("maskFor", "maskWith.text", "maskWith.user", "maskWith.read")
                        ++ $pull("del".$in(orig :: (!send.mute).option(dest).toList))
                    // keep maskWith.date always valid (though sometimes redundant)
                    // unset "deleted by receiver" unless the message is muted
                  )
            for
              _ <- msgWrite
              _ <- threadWrite
            yield
              import MsgSecurity.*
              import lila.core.socket.SendTo
              import lila.core.socket.makeMessage
              if send == Ok || send == TrollFriend then
                notifier.onPost(threadId)
                Bus.pub(SendTo(dest, makeMessage("msgNew", json.renderMsg(msg))))
              if send == Ok && !multi then shutupApi.privateMessage(orig, dest, text)
              PostResult.Success
      yield res
    }

  def lastDirectMsg(threadId: MsgThread.Id, maskFor: UserId): Fu[Option[Msg.Last]] =
    colls.thread
      .one[MsgThread]($id(threadId))
      .mapz: doc =>
        if doc.maskFor.contains(maskFor) then doc.maskWith
        else Some(doc.lastMsg)

  def setRead(userId: UserId, contactId: UserId): Funit =
    val threadId = MsgThread.id(userId, contactId)
    colls.thread
      .updateField(
        $id(threadId) ++ $doc("lastMsg.user" -> contactId),
        "lastMsg.read",
        true
      )
      .flatMap: res =>
        (res.nModified > 0).so(notifier.onRead(threadId, userId, contactId))

  def postPreset(destId: UserId, preset: lila.core.msg.MsgPreset): Fu[PostResult] =
    systemPost(destId, preset.text)

  def systemPost(destId: UserId, text: String) =
    post(UserId.lichess, destId, text, multi = true, ignoreSecurity = true)

  def multiPost(destSource: Source[UserId, ?], text: String)(using me: Me): Fu[Int] =
    val now = nowInstant // same timestamp on all
    destSource
      .filterNot(_.is(me))
      .mapAsync(4):
        post(me, _, text, multi = true, date = now)
          .logFailure(logger)
          .recoverDefault(PostResult.Invalid)
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()

  def cliMultiPost(orig: UserStr, dests: Seq[UserId], text: String): Fu[String] =
    userApi
      .me(orig)
      .flatMap:
        case None => fuccess(s"Unknown sender $orig")
        case Some(me) => multiPost(Source(dests), text)(using me).inject("done")

  def recentByForMod(user: User, nb: Int): Fu[List[ModMsgConvo]] =
    colls.thread
      .aggregateList(nb): framework =>
        import framework.*
        Match($doc("users" -> user.id)) -> List(
          Sort(Descending("lastMsg.date")),
          Limit(nb),
          UnwindField("users"),
          Match($doc("users".$ne(user.id))),
          PipelineOperator:
            $lookup.simple(
              from = colls.msg,
              as = "msgs",
              local = "_id",
              foreign = "tid",
              pipe = List(
                $doc("$sort" -> $sort.desc("date")),
                $doc("$limit" -> 11),
                $doc("$project" -> msgProjection)
              )
            )
          ,
          PipelineOperator:
            $lookup.simple(from = userRepo.coll, as = "contact", local = "users", foreign = "_id")
          ,
          UnwindField("contact")
        )
      .flatMap: docs =>
        import userRepo.userHandler
        val convos = for
          doc <- docs
          msgs <- doc.getAsOpt[List[Msg]]("msgs")
          contact <- doc.getAsOpt[User]("contact")
        yield (contact, msgs)
        convos.sequentially: (contact, msgs) =>
          relationApi.fetchRelation(contact.id, user.id).map { relation =>
            ModMsgConvo(contact, msgs.take(10), Relations(relation, none), msgs.length == 11)
          }

  def deleteAllBy(user: User): Funit = for
    threads <- colls.thread.list[MsgThread]($doc("users" -> user.id))
    _ <- colls.thread.delete.one($doc("users" -> user.id))
    _ <- colls.msg.delete.one($doc("tid".$in(threads.map(_.id))))
    _ <- notifier.deleteAllBy(threads, user)
  yield ()

  private val msgProjection = $doc("_id" -> false, "tid" -> false)

  private def threadMsgsFor(threadId: MsgThread.Id, me: User, before: Option[Instant]): Fu[List[Msg]] =
    colls.msg
      .find(
        $doc("tid" -> threadId) ++ before.so: b =>
          $doc("date".$lt(b)),
        msgProjection.some
      )
      .sort($sort.desc("date"))
      .cursor[Bdoc]()
      .list(msgsPerPage.value)
      .map:
        _.flatMap: doc =>
          doc.getAsOpt[List[UserId]]("del").forall(!_.has(me.id)).so(doc.asOpt[Msg])

  private def setReadBy(threadId: MsgThread.Id, me: User, contactId: UserId): Funit =
    colls.thread
      .updateField(
        $id(threadId) ++ $doc(
          "lastMsg.user".$ne(me.id),
          "lastMsg.read" -> false
        ),
        "lastMsg.read",
        true
      )
      .flatMap: res =>
        (res.nModified > 0).so(notifier.onRead(threadId, me.id, contactId))

  def allMessagesOf(userId: UserId): Source[(String, Instant), ?] =
    colls.thread
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match($doc("users" -> userId)),
          Project($id(true)),
          PipelineOperator:
            $lookup.pipelineFull(
              from = colls.msg.name,
              as = "msg",
              let = $doc("t" -> "$_id"),
              pipe = List:
                $doc:
                  "$match" ->
                    $expr:
                      $and(
                        $doc("$eq" -> $arr("$user", userId)),
                        $doc("$eq" -> $arr("$tid", "$$t")),
                        excludeTeamMessages
                      )
            )
          ,
          Unwind("msg"),
          Project($doc("_id" -> false, "msg.text" -> true, "msg.date" -> true))
        )
      .documentSource()
      .mapConcat: doc =>
        (for
          msg <- doc.child("msg")
          text <- msg.string("text")
          date <- msg.getAsOpt[Instant]("date")
        yield (text, date)).toList

  def msgsToReport(from: UserId, onlyIds: Option[List[lila.core.msg.ID]] = None)(using
      me: Me
  ): Fu[List[IdText]] =
    colls.msg
      .find(
        $doc("tid" -> MsgThread.id(from, me.userId), "user" -> from.id) ++ onlyIds.so($inIds),
        $doc("text" -> true).some
      )
      .sort($sort.desc("date"))
      .cursor[Bdoc]()
      .list(200)
      .map: docs =>
        for
          doc <- docs
          text <- doc.getAsOpt[String]("text")
          id <- doc.getAsOpt[String]("_id")
        yield IdText(id, text)

  private val excludeTeamMessages = $doc:
    "$not" -> $doc:
      "$regexMatch" -> $doc(
        "input" -> "$text",
        "regex" -> "You received this because you are (subscribed to messages|part) of the team"
      )

  // include responses from the other user
  def modFullCommsExport(userId: UserId): Source[(MsgThread.Id, NonEmptyList[Msg]), ?] =
    colls.thread
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match($doc("users" -> userId)),
          Sort(Descending("lastMsg.date")),
          Project($id(true)),
          PipelineOperator:
            $lookup.pipelineFull(
              from = colls.msg.name,
              as = "msgs",
              let = $doc("t" -> "$_id"),
              pipe = List(
                $doc(
                  "$match" ->
                    $expr:
                      $and(
                        $doc("$eq" -> $arr("$tid", "$$t")),
                        excludeTeamMessages
                      )
                ),
                $doc("$sort" -> $sort.desc("date"))
              )
            )
        )
      .documentSource()
      .mapConcat: doc =>
        (for
          tid <- doc.getAsOpt[MsgThread.Id]("_id")
          // filter conversation where only team messages where sent
          msgs <- doc.getAsOpt[NonEmptyList[Msg]]("msgs")
        yield (tid, msgs)).toList
