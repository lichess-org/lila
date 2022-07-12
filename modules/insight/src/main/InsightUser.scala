package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User

case class InsightUser(
    _id: User.ID, // user id
    count: Int,   // nb insight entries
    families: List[LilaOpeningFamily],
    openings: List[LilaOpening],
    lastSeen: DateTime
) {

  def openingFamilies = openings

  def id = _id
}

object InsightUser {

  def make(userId: User.ID, count: Int, families: List[LilaOpeningFamily], openings: List[LilaOpening]) =
    InsightUser(userId, count, families, openings, DateTime.now)
}

final class InsightUserApi(coll: AsyncColl)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val userCacheBSONHandler = Macros.handler[InsightUser]

  def find(id: User.ID) = coll(_.one[InsightUser]($id(id)))

  def save(u: InsightUser) = coll(_.update.one($id(u.id), u, upsert = true).void)

  def setSeenNow(u: User): Funit =
    coll(_.updateField($id(u.id), "lastSeen", DateTime.now).void)

  def remove(id: String) = coll(_.delete.one($id(id)).void)
}
