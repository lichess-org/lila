package lila.appeal

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

final class AppealApi(
    coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def mine(me: User): Fu[Option[Appeal]] = coll.byId[Appeal](me.id)

  def get(user: User) = coll.byId[Appeal](user.id)

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
            updatedAt = DateTime.now
          )
        coll.insert.one(appeal) inject appeal
      case Some(prev) =>
        val appeal = prev.post(text, me)
        coll.update.one($id(appeal.id), appeal) inject appeal
    }

  def reply(text: String, prev: Appeal, mod: User) = {
    val appeal = prev.post(text, mod)
    coll.update.one($id(appeal.id), appeal) inject appeal
  }

  def close(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.close).void

  def open(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.open).void

  def mute(appeal: Appeal) =
    coll.update.one($id(appeal.id), appeal.mute).void
}
