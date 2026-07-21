package lila.appeal

import lila.appeal.Appeal.Id as AppealId
import lila.core.userId.ModId
import lila.db.dsl.{ *, given }

final class AppealApi(
    coll: Coll,
    snoozer: lila.memo.Snoozer[Appeal.SnoozeKey]
)(using Executor):

  import BsonHandlers.given

  def byTopic[U: UserIdOf](u: U): Fu[Appeal.ByTopic] =
    findAll(u).map(_.groupBy(_.topic).view.mapValues(_.head).toMap)

  def latestBy[U: UserIdOf](u: U): Fu[Option[Appeal]] =
    findAll(u).map(_.headOption)

  def find[U: UserIdOf](u: U, topic: AppealTopic): Fu[Option[Appeal]] =
    coll.find($doc("user" -> u.id, "topic" -> topic)).one[Appeal]

  def findAll[U: UserIdOf](u: U): Fu[List[Appeal]] =
    coll.find($doc("user" -> u.id)).sort($sort.desc("updatedAt")).cursor[Appeal]().listAll()

  def byUserIds(userIds: List[UserId]): Fu[List[Appeal]] =
    coll.find($doc("user".$in(userIds))).cursor[Appeal]().listAll()

  def exists(user: User) = coll.exists($id(user.id))

  def post(topic: AppealTopic, text: String)(using me: Me) =
    find(me, topic).flatMap:
      case None =>
        val appeal = Appeal.make(topic, text)
        coll.insert.one(appeal).inject(appeal)
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal).inject(appeal)

  def reply(text: String, prev: Appeal)(using me: MyId) =
    val appeal = prev.post(text, me)
    for _ <- coll.update.one($id(appeal.id), appeal) yield appeal

  def countUnread = coll.secondary.countSel($doc("status" -> Appeal.Status.unread))

  def countUnreadByTopic: Fu[Map[AppealTopic, Int]] =
    coll
      .aggregateList(50, _.sec): framework =>
        import framework.*
        Match($doc("status" -> Appeal.Status.unread)) ->
          List(PipelineOperator($doc("$sortByCount" -> "$topic")))
      .map: docs =>
        for
          doc <- docs
          topic <- doc.getAsOpt[AppealTopic]("_id")
          count <- doc.int("count")
        yield topic -> count
      .map(_.toMap)

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
          userId <- doc.getAsOpt[UserId]("user")
          msg <- doc.getAsOpt[AppealMsg]("msgs")
        yield userId -> msg

  def myQueue(topic: Option[AppealTopic], nb: Int = 50)(using me: Me): Fu[List[Appeal]] =
    val snoozedIds = snoozer.snoozedKeysOf(me.userId).map(_.appealId)
    val selector =
      $doc("status" -> Appeal.Status.unread) ++
        snoozedIds.nonEmpty.so($doc("_id".$nin(snoozedIds))) ++
        topic.so(t => $doc("topic" -> t))
    coll
      .find(selector)
      .sort($sort.asc("firstUnrepliedAt"))
      .cursor[Appeal]()
      .list(nb * 2)
      .map(_.sortBy(a => (!a.modIds.contains(me.userId), a.firstUnrepliedAt)))
      .map(_.take(nb))

  def setReadIfUnread(user: UserId, topic: AppealTopic) =
    coll
      .updateField(
        $doc("user" -> user, "topic" -> topic, "status" -> Appeal.Status.unread),
        "status",
        Appeal.Status.read
      )
      .void

  private def update(appeal: Appeal): Fu[Appeal] =
    coll.update.one($id(appeal.id), appeal).inject(appeal)

  def toggleClosed(appeal: Appeal, v: Boolean, sleepMonths: Int) =
    for
      a2 <- update(appeal.toggleClosed(v))
      _ <- (v && sleepMonths > 0).so:
        update(a2.sleep(sleepMonths.some)).void
    yield ()

  def toggleRead(appeal: Appeal, v: Boolean) = update(appeal.toggleRead(v)).void

  def toggleClosed(user: UserId, topic: AppealTopic, v: Boolean, sleepMonths: Int = 0): Funit =
    find(user, topic).flatMapz(toggleClosed(_, v, sleepMonths))

  def toggleClosedAllOf(user: UserId, v: Boolean): Funit =
    findAll(user).flatMap(_.sequentiallyVoid(toggleClosed(_, v, 0)))

  def setReadById(userId: UserId) = for
    appeals <- findAll(userId)
    _ <- appeals.sequentiallyVoid: appeal =>
      setReadIfUnread(userId, appeal.topic)
  yield ()

  def setUnreadBy(userId: UserId, topic: AppealTopic): Funit =
    find(userId, topic).flatMapz: a =>
      update(a.unread).void

  def onAccountClose(user: User) = setReadById(user.id)

  def snooze(appealId: AppealId, duration: String)(using mod: Me): Unit =
    snoozer.set(Appeal.SnoozeKey(mod.userId, appealId), duration)

  private[appeal] def reopenPausedAppeals(): Funit = for
    appeals <- coll.list[Appeal]("closedUntil".$lt(nowInstant), 20)
    _ <- appeals.sequentiallyVoid: appeal =>
      update(appeal.toggleClosed(false))
  yield ()
