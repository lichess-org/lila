package lila.coach

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import scala.concurrent.duration._
import scalaz.NonEmptyList

import lila.db.BSON._
import lila.db.Implicits._
import lila.user.UserRepo

private final class Storage(coll: Coll) {

  import Storage._
  import BSONHandlers._

  def aggregate(operators: NonEmptyList[PipelineOperator]): Fu[AggregationResult] =
    coll.aggregate(operators.head, operators.tail)

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

object Storage {
  def selectId(id: String) = BSONDocument("_id" -> id)
  def selectUserId(id: String) = BSONDocument("userId" -> id)
  val sortChronological = BSONDocument("date" -> 1)
  val sortAntiChronological = BSONDocument("date" -> -1)

  def combineDocs(docs: List[BSONDocument]) = docs.foldLeft(BSONDocument()) {
    case (acc, doc) => acc ++ doc
  }
}
