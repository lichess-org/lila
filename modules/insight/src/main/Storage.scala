package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

private final class Storage(coll: Coll) {

  import Storage._
  import BSONHandlers._
  import Entry.{ BSONFields => F }

  def aggregate(operators: NonEmptyList[PipelineOperator]): Fu[List[Bdoc]] =
    coll.aggregateList(
      operators.head,
      operators.tail.toList,
      maxDocs = Int.MaxValue,
      allowDiskUse = true
    )

  def fetchFirst(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortChronological).one[Entry]

  def fetchLast(userId: String): Fu[Option[Entry]] =
    coll.find(selectUserId(userId)).sort(sortAntiChronological).one[Entry]

  def count(userId: String): Fu[Int] = coll.countSel(selectUserId(userId))

  def insert(p: Entry): Funit = coll.insert.one(p).void

  def bulkInsert(ps: Seq[Entry]) = coll.insert.many(ps)

  def update(p: Entry): Funit = coll.update.one(
    q = selectId(p.id), u = p, upsert = true
  ).void

  def remove(p: Entry): Funit = coll.delete.one(selectId(p.id)).void

  def removeAll(userId: String): Funit =
    coll.delete.one(selectUserId(userId)).void

  def find(id: String) = coll.find(selectId(id)).one[Entry]

  def ecos(userId: String): Fu[Set[String]] =
    coll.distinct[String, Set](F.eco, selectUserId(userId))

  def nbByPerf(userId: String): Fu[Map[PerfType, Int]] = coll.aggregateList(
    Match(BSONDocument(F.userId -> userId)),
    List(GroupField(F.perf)("nb" -> SumValue(1))),
    maxDocs = 50
  ).map {
      _.flatMap { doc =>
        for {
          perfType <- doc.getAs[PerfType]("_id")
          nb <- doc.getAs[Int]("nb")
        } yield perfType -> nb
      }(scala.collection.breakOut)
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
