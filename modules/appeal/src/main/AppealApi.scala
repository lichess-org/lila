package lila.appeal

import org.joda.time.DateTime

import lila.db.dsl._
import lila.user.{ Holder, NoteApi, User, UserRepo }
import reactivemongo.api.ReadPreference

final class AppealApi(
    coll: Coll,
    userRepo: UserRepo,
    noteApi: NoteApi,
    snoozer: lila.memo.Snoozer[Appeal.SnoozeKey]
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def mine(me: User): Fu[Option[Appeal]] = coll.byId[Appeal](me.id)

  def get(user: User) = coll.byId[Appeal](user.id)

  def byUserIds(userIds: List[User.ID]) = coll.byIds[Appeal](userIds)

  def byId(appealId: User.ID) = coll.byId[Appeal](appealId)

  def exists(user: User) = coll.exists($id(user.id))

  def post(text: String, me: User) =
    mine(me) flatMap {
      case None =>
        val appeal =
          Appeal(
            _id = me.id,
            msgs = Vector(
              AppealMsg(
                by = me.id,
                text = text,
                at = DateTime.now
              )
            ),
            status = Appeal.Status.Unread,
            createdAt = DateTime.now,
            updatedAt = DateTime.now,
            firstUnrepliedAt = DateTime.now
          )
        coll.insert.one(appeal) inject appeal
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal) inject appeal
    }

  def reply(text: String, prev: Appeal, mod: Holder, preset: Option[String]) = {
    val appeal = prev.post(text, mod.user)
    coll.update.one($id(appeal.id), appeal) >> {
      preset ?? { note =>
        userRepo.byId(appeal.id) flatMap {
          _ ?? { noteApi.write(_, s"Appeal reply: $note", mod.user, modOnly = true, dox = false) }
        }
      }
    } inject appeal
  }

  def countUnread = coll.countSel($doc("status" -> Appeal.Status.Unread.key))

  def queueOf(mod: User) = bothQueues(snoozer snoozedKeysOf mod.id map (_.appealId))

  private def bothQueues(exceptIds: Iterable[User.ID]): Fu[List[Appeal.WithUser]] =
    fetchQueue(
      selector = $doc("status" -> Appeal.Status.Unread.key) ++ {
        exceptIds.nonEmpty ?? $doc("_id" $nin exceptIds)
      },
      ascending = true,
      nb = 30
    ) flatMap { unreads =>
      fetchQueue(
        selector = $doc("status" $ne Appeal.Status.Unread.key),
        ascending = false,
        nb = 40 - unreads.size
      ) map { unreads ::: _ }
    }

  private def fetchQueue(selector: Bdoc, ascending: Boolean, nb: Int): Fu[List[Appeal.WithUser]] =
    coll
      .aggregateList(
        maxDocs = nb,
        ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match(selector) -> List(
          Sort((if (ascending) Ascending.apply _ else Descending.apply _)("firstUnrepliedAt")),
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
      }
      .map { docs =>
        for {
          doc    <- docs
          appeal <- doc.asOpt[Appeal]
          user   <- doc.getAsOpt[User]("user")
        } yield Appeal.WithUser(appeal, user)
      }

  def setRead(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.read).void

  def setUnread(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.unread).void

  def toggleMute(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.toggleMute).void

  def setReadById(userId: User.ID) =
    byId(userId) flatMap { _ ?? setRead }

  def setUnreadById(userId: User.ID) =
    byId(userId) flatMap { _ ?? setUnread }

  def onAccountClose(user: User) = setReadById(user.id)

  def snooze(mod: User, appealId: User.ID, duration: String): Unit =
    snoozer.set(Appeal.SnoozeKey(mod.id, appealId), duration)
}
