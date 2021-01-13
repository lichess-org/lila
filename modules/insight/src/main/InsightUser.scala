package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.db.AsyncColl
import lila.user.User

case class InsightUser(
    _id: User.ID, // user id
    count: Int,   // nb insight entries
    ecos: Set[String],
    lastSeen: DateTime
) {

  def id = _id
}

object InsightUser {

  def make(userId: User.ID, count: Int, ecos: Set[String]) =
    InsightUser(userId, count, ecos, DateTime.now)
}

final private class InsightUserApi(coll: AsyncColl)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val userCacheBSONHandler = Macros.handler[InsightUser]

  def find(id: String) = coll(_.one[InsightUser]($id(id)))

  def save(u: InsightUser) = coll(_.update.one($id(u.id), u, upsert = true).void)

  def setSeenNow(u: User): Funit =
    coll(_.updateField($id(u.id), "lastSeen", DateTime.now).void)

  def remove(id: String) = coll(_.delete.one($id(id)).void)
}
