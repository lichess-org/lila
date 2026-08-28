package lila.appeal

import lila.appeal.Appeal.Id as AppealId
import lila.common.Bus
import lila.core.userId.ModId
import lila.db.dsl.{ *, given }
import lila.appeal.AppealEventForm.{ ChoiceData, MessageData }

final class AppealApi(
    coll: Coll,
    snoozer: lila.memo.Snoozer[Appeal.SnoozeKey]
)(using Executor):

  import BsonHandlers.given

  def byTopic[U: UserIdOf](u: U): Fu[UserAppeals] = UserAppeals.from:
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

  def post(topic: AppealTopic, data: AppealForm.Data, appeals: UserAppeals)(using me: Me) =
    appeals.get(topic) match
      case None =>
        val appeal = Appeal.make(topic, data.text, data.accounts)
        coll.insert.one(appeal).inject(appeal)
      case Some(prev) =>
        val appeal = prev.postLegacyMessage(data.text, me, appeals.muted)
        coll.update.one($id(appeal.id), appeal).inject(appeal)

  def postChoiceEvent(appeal: Appeal, data: ChoiceData)(using me: MyId): Fu[Option[Appeal]] =
    validateChoiceEvent(appeal, data).so: event =>
      postEvent(appeal, event).map(_.some)

  private def validateChoiceEvent(appeal: Appeal, data: ChoiceData)(using me: MyId): Option[ChoiceEvent] =
    val expectedAnswerer = if appeal.user.isnt(me) then Answerer.Mod else Answerer.User
    appeal.nextNode
      .so:
        case cn: ChoiceNode if cn.id == data.nodeId && cn.answerer == expectedAnswerer =>
          cn.getAnswerBranch(data.answerId)
            .map: b =>
              ChoiceEvent(me, cn.id, cn.question, data.answerId, b.answer, nowInstant)
        case _ => none

  private def postEvent(appeal: Appeal, event: AppealMsg): Fu[Appeal] =
    val (advancedAppeal, effects) = autoAdvance(appeal.postEvent(event))
    for savedAppeal <- update(advancedAppeal)
    yield
      effects.foreach(publishEffect(savedAppeal, _))
      savedAppeal

  private def autoAdvance(appeal: Appeal): (Appeal, List[AppealEffect]) =
    appeal.nextNode match
      case Some(node: ActionNode) =>
        val effects = node.effects.getOrElse(Nil)
        val newAppeal = appeal.postEvent(ActionEvent(UserId.lichess, node.id, node.text, nowInstant))
        val appealAfterEffects = effects.foldLeft(newAppeal)(applyEffect)
        (appealAfterEffects, effects)
      case _ => (appeal, Nil)

  private def applyEffect(appeal: Appeal, effect: AppealEffect): Appeal =
    effect match
      case AppealEffect.Sleep(months) => appeal.toggleClosed(true).sleep(months.some)
      case AppealEffect.Close => appeal.toggleClosed(true)
      case AppealEffect.Unmark => appeal

  private def publishEffect(appeal: Appeal, effect: AppealEffect): Unit =
    effect match
      case AppealEffect.Unmark => Bus.pub(lila.core.mod.UndoMark(appeal.user, appeal.topic))
      case _ => ()

  def postMessageEvent(appeal: Appeal, data: MessageData)(using me: MyId): Fu[Appeal] =
    update(appeal.postEvent(MessageEvent(me, data, nowInstant)))

  def withdraw(appeal: Appeal): Funit = update(appeal.withdraw).void

  def modReply(text: String, prev: Appeal)(using me: MyId) =
    val appeal = prev.postLegacyMessage(text, me, muted = false)
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
      .map(_.sortBy(a => (!a.participated(me.userId), a.firstUnrepliedAt)))
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

  // just one muted appeal makes all user appeals muted
  def toggleMute(user: UserId, v: Boolean): Funit =
    if v then
      coll.update
        .one(
          $doc("user" -> user),
          $set("muted" -> true, "status" -> Appeal.Status.read),
          multi = true
        )
        .void
    else coll.update.one($doc("user" -> user), $unset("muted"), multi = true).void

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
