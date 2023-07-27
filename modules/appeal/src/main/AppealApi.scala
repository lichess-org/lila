package lila.appeal

import lila.db.dsl.{ given, * }
import lila.user.{ Me, NoteApi, User, UserRepo }
import lila.user.Me

final class AppealApi(
    coll: Coll,
    userRepo: UserRepo,
    noteApi: NoteApi,
    snoozer: lila.memo.Snoozer[Appeal.SnoozeKey]
)(using Executor):

  import BsonHandlers.given

  def byId[U: UserIdOf](u: U): Fu[Option[Appeal]] = coll.byId[Appeal](u.id)

  def byUserIds(userIds: List[UserId]) = coll.byIds[Appeal, UserId](userIds)

  def exists(user: User) = coll.exists($id(user.id))

  def post(text: String)(using me: Me) =
    byId(me).flatMap:
      case None =>
        val appeal =
          Appeal(
            id = me.userId into Appeal.Id,
            msgs = Vector(
              AppealMsg(
                by = me,
                text = text,
                at = nowInstant
              )
            ),
            status = Appeal.Status.Unread,
            createdAt = nowInstant,
            updatedAt = nowInstant,
            firstUnrepliedAt = nowInstant
          )
        coll.insert.one(appeal) inject appeal
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal) inject appeal

  def reply(text: String, prev: Appeal, preset: Option[String])(using me: Me) =
    val appeal = prev.post(text, me.value)
    coll.update.one($id(appeal.id), appeal) >> {
      preset.so: note =>
        userRepo.byId(appeal.id) flatMapz {
          noteApi.write(_, s"Appeal reply: $note", modOnly = true, dox = false)
        }
    } inject appeal

  def countUnread = coll.countSel($doc("status" -> Appeal.Status.Unread.key))

  def myQueue(using me: Me) = bothQueues(snoozer snoozedKeysOf me.userId map (_.appealId.userId))

  private def bothQueues(exceptIds: Iterable[UserId]): Fu[List[Appeal.WithUser]] =
    fetchQueue(
      selector = $doc("status" -> Appeal.Status.Unread.key) ++ {
        exceptIds.nonEmpty so $doc("_id" $nin exceptIds)
      },
      ascending = true,
      nb = 50
    ) flatMap { unreads =>
      fetchQueue(
        selector = $doc("status" $ne Appeal.Status.Unread.key),
        ascending = false,
        nb = 60 - unreads.size
      ) map { unreads ::: _ }
    }

  private def fetchQueue(selector: Bdoc, ascending: Boolean, nb: Int): Fu[List[Appeal.WithUser]] =
    coll
      .aggregateList(maxDocs = nb, _.sec): framework =>
        import framework.*
        Match(selector) -> List(
          Sort((if ascending then Ascending.apply else Descending.apply) ("firstUnrepliedAt")),
          Limit(nb),
          PipelineOperator(
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = "_id",
              foreign = "_id"
            )
          ),
          UnwindField("user")
        )
      .map: docs =>
        for
          doc    <- docs
          appeal <- doc.asOpt[Appeal]
          user   <- doc.getAsOpt[User]("user")
        yield Appeal.WithUser(appeal, user)

  def setRead(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.read).void

  def setUnread(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.unread).void

  def toggleMute(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.toggleMute).void

  def setReadById(userId: UserId) =
    byId(userId) flatMapz setRead

  def setUnreadById(userId: UserId) =
    byId(userId) flatMapz setUnread

  def onAccountClose(user: User) = setReadById(user.id)

  def snooze(appealId: Appeal.Id, duration: String)(using mod: Me): Unit =
    snoozer.set(Appeal.SnoozeKey(mod.userId, appealId), duration)
