package lila.insight

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._
import scalaz.NonEmptyList

import lila.db.BSON._
import lila.db.Implicits._
import lila.user.UserRepo
import lila.rating.PerfType

private final class Storage(coll: Coll) {

  import Storage._
  import BSONHandlers._
  import Entry.{ BSONFields => F }

  def aggregate(operators: NonEmptyList[PipelineOperator]): Fu[AggregationResult] =
    coll.aggregate(operators.head, operators.tail, allowDiskUse = true)

  def fetchFirst(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortChronological).one[Entry]

  def fetchLast(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortAntiChronological).one[Entry]

  def count(userId: String): Fu[Int] =
    coll.count(selectUserId(userId).some)

  def insert(p: Entry) = coll.insert(p).void

  def bulkInsert(ps: Seq[Entry]) = coll.bulkInsert(
    documents = ps.map(BSONHandlers.EntryBSONHandler.write).toStream,
    ordered = false)

  def update(p: Entry) = coll.update(selectId(p.id), p, upsert = true).void

  def remove(p: Entry) = coll.remove(selectId(p.id)).void

  def removeAll(userId: String) = coll.remove(selectUserId(userId)).void

  def find(id: String) = coll.find(selectId(id)).one[Entry]

  def ecos(userId: String): Fu[Set[String]] =
    coll.distinct(F.eco, selectUserId(userId).some).map {
      _.collect { case BSONString(eco) => eco } toSet
    }

  def nbByPerf(userId: String): Fu[Map[PerfType, Int]] = coll.aggregate(
    Match(BSONDocument(F.userId -> userId)),
    List(GroupField(F.perf)("nb" -> SumValue(1)))
  ).map {
      _.documents.flatMap { doc =>
        for {
          perfType <- doc.getAs[PerfType]("_id")
          nb <- doc.getAs[Int]("nb")
        } yield perfType -> nb
      }.toMap
    }
}

private object Storage {

  import Entry.{ BSONFields => F }

  def selectId(id: String) = BSONDocument(F.id -> id)
  def selectUserId(id: String) = BSONDocument(F.userId -> id)
  val sortChronological = BSONDocument(F.date -> 1)
  val sortAntiChronological = BSONDocument(F.date -> -1)

  def combineDocs(docs: List[BSONDocument]) = docs.foldLeft(BSONDocument()) {
    case (acc, doc) => acc ++ doc
  }
}
