package lila.appeal

import lila.appeal.Appeal.Filter
import lila.core.id.AppealId
import lila.core.user.{ UserMark, UserRepo }
import lila.core.userId.ModId
import lila.db.dsl.{ *, given }

final class AppealApi(
    coll: Coll,
    userRepo: UserRepo,
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
            id = me.userId.into(AppealId),
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
        coll.insert.one(appeal).inject(appeal)
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal).inject(appeal)

  def reply(text: String, prev: Appeal)(using me: MyId) =
    val appeal = prev.post(text, me)
    for _ <- coll.update.one($id(appeal.id), appeal) yield appeal

  def countUnread = coll.countSel($doc("status" -> Appeal.Status.Unread.key))

  def logsOf(since: Instant, mod: ModId): Fu[List[(UserId, AppealMsg)]] =
    coll
      .aggregateList(maxDocs = 50, _.sec): framework =>
        import framework.*
        Match($doc("msgs.by" -> mod)) -> List(
          Project($doc("msgs" -> 1)),
          Unwind("msgs"),
          Match($doc("msgs.by" -> mod, "msgs.at".$gt(since))),
          Sort(Descending("msgs.at")),
          Limit(50)
        )
      .map: docs =>
        for
          doc <- docs
          userId <- doc.getAsOpt[UserId]("_id")
          msg <- doc.getAsOpt[AppealMsg]("msgs")
        yield userId -> msg

  def myQueue(filter: Option[Filter])(using me: Me) =
    bothQueues(filter, snoozer.snoozedKeysOf(me.userId).map(_.appealId.userId))

  private def bothQueues(
      filter: Option[Filter],
      exceptIds: Iterable[UserId]
  ): Fu[List[Appeal.WithUser]] =
    fetchQueue(
      selector = $doc("status" -> Appeal.Status.Unread.key) ++ {
        exceptIds.nonEmpty.so($doc("_id".$nin(exceptIds)))
      },
      filter = filter,
      ascending = true,
      nb = 50
    ).flatMap { unreads =>
      fetchQueue(
        selector = $doc("status".$ne(Appeal.Status.Unread.key)),
        filter = filter,
        ascending = false,
        nb = 60 - unreads.size
      ).map { unreads ::: _ }
    }

  private def fetchQueue(
      selector: Bdoc,
      filter: Option[Filter],
      ascending: Boolean,
      nb: Int
  ): Fu[List[Appeal.WithUser]] =
    coll
      .aggregateList(maxDocs = nb, _.sec): framework =>
        import framework.*
        Match(selector) -> List(
          Sort((if ascending then Ascending.apply else Descending.apply) ("firstUnrepliedAt")),
          Limit(nb * 20),
          PipelineOperator:
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = "_id",
              foreign = "_id",
              pipe = filter.so(f => List($doc("$match" -> filterSelector(f))))
            )
          ,
          Limit(nb),
          UnwindField("user")
        )
      .map: docs =>
        import userRepo.userHandler
        for
          doc <- docs
          appeal <- doc.asOpt[Appeal]
          user <- doc.getAsOpt[User]("user")
        yield Appeal.WithUser(appeal, user)

  def filterSelector(filter: Filter) =
    import lila.core.user.BSONFields as F
    filter.value match
      case Some(mark) => $doc(F.marks.$in(List(mark.key)))
      case None => $doc(F.marks.$nin(UserMark.bannable))

  def setRead(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.read).void

  def setUnread(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.unread).void

  def toggleMute(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.toggleMute).void

  def setReadById(userId: UserId) =
    byId(userId).flatMapz(setRead)

  def setUnreadById(userId: UserId) =
    byId(userId).flatMapz(setUnread)

  def onAccountClose(user: User) = setReadById(user.id)

  def snooze(appealId: AppealId, duration: String)(using mod: Me): Unit =
    snoozer.set(Appeal.SnoozeKey(mod.userId, appealId), duration)

  object modFilter:
    private var store = Map.empty[UserId, Option[Filter]]
    def fromQuery(str: Option[String])(using me: Me): Option[Filter] =
      if str.has("reset") then store = store - me.userId
      val filter = str.map(Filter.byName.get) | store.get(me.userId).flatten
      store = store + (me.userId -> filter)
      filter
