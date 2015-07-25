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

final class StatApi(coll: Coll) {

  import BSONHandlers._

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectUserId(id: String) = BSONDocument("userId" -> id)
  private val sortChronological = BSONDocument("from" -> 1)
  private val sortAntiChronological = BSONDocument("from" -> -1)

  def fetchRange(userId: String, range: Option[Range]): Fu[Option[Period]] =
    range.fold(fetchAll(userId)) { r =>
      coll.find(selectUserId(userId))
        .skip(r.min)
        .sort(sortChronological)
        .cursor[Period]()
        .enumerate(r.size) &>
        Enumeratee.take(r.size) |>>>
        Iteratee.fold[Period, Option[Period]](none) {
          case (a, b) => a.fold(b)(_ merge b).some
        }
    }

  def fetchAll(userId: String): Fu[Option[Period]] =
    fetchRange(userId, Range(0, 1000).some)

  def fetchFirst(userId: String): Fu[Option[Period]] =
    fetchRange(userId, Range(0, 1).some)

  def fetchLast(userId: String): Fu[Option[Period]] =
    coll.find(selectUserId(userId)).sort(sortAntiChronological).one[Period]

  def count(userId: String): Fu[Int] =
    coll.count(selectUserId(userId).some)

  def insert(p: Period) = coll.insert(p).void

  def remove(p: Period) = coll.remove(selectId(p.id)).void

  def removeAll(userId: String) = coll.remove(selectUserId(userId)).void
}
