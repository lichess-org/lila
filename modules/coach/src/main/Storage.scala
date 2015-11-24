package lila.coach

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Implicits._
import lila.user.UserRepo

final class Storage(coll: Coll) {

  import BSONHandlers._

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectUserId(id: String) = BSONDocument("userId" -> id)
  private val sortChronological = BSONDocument("date" -> 1)
  private val sortAntiChronological = BSONDocument("date" -> -1)

  private def fetchRange(userId: String, range: Range): Fu[List[Entry]] =
    coll.find(selectUserId(userId))
      .skip(range.min)
      .sort(sortChronological)
      .cursor[Entry]()
      .collect[List](range.size)

  def fetchFirst(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortChronological).one[Entry]

  def fetchLast(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortAntiChronological).one[Entry]

  def count(userId: String): Fu[Int] =
    coll.count(selectUserId(userId).some)

  def insert(p: Entry) = coll.insert(p).void

  def remove(p: Entry) = coll.remove(selectId(p.id)).void

  def removeAll(userId: String) = coll.remove(selectUserId(userId)).void
}
