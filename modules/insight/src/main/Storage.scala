package lila.insight

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.db.AsyncColl
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final private class Storage(val coll: AsyncColl)(implicit ec: scala.concurrent.ExecutionContext) {

  import Storage._
  import BSONHandlers._
  import InsightEntry.{ BSONFields => F }

  def fetchFirst(userId: String): Fu[Option[InsightEntry]] =
    coll(_.find(selectUserId(userId)).sort(sortChronological).one[InsightEntry])

  def fetchLast(userId: String): Fu[Option[InsightEntry]] =
    coll(_.find(selectUserId(userId)).sort(sortAntiChronological).one[InsightEntry])

  def count(userId: String): Fu[Int] =
    coll(_.countSel(selectUserId(userId)))

  def insert(p: InsightEntry) = coll(_.insert.one(p).void)

  def bulkInsert(ps: Seq[InsightEntry]) =
    coll {
      _.insert.many(
        ps.flatMap(BSONHandlers.EntryBSONHandler.writeOpt)
      )
    }

  def update(p: InsightEntry) = coll(_.update.one(selectId(p.id), p, upsert = true).void)

  def remove(p: InsightEntry) = coll(_.delete.one(selectId(p.id)).void)

  def removeAll(userId: String) = coll(_.delete.one(selectUserId(userId)).void)

  def find(id: String) = coll(_.one[InsightEntry](selectId(id)))

  def ecos(userId: String): Fu[Set[String]] =
    coll {
      _.distinctEasy[String, Set](F.eco, selectUserId(userId))
    }

  def nbByPerf(userId: String): Fu[Map[PerfType, Int]] =
    coll {
      _.aggregateList(
        maxDocs = 50
      ) { framework =>
        import framework._
        Match(BSONDocument(F.userId -> userId)) -> List(
          GroupField(F.perf)("nb" -> SumAll)
        )
      }.map {
        _.flatMap { doc =>
          for {
            perfType <- doc.getAsOpt[PerfType]("_id")
            nb       <- doc.int("nb")
          } yield perfType -> nb
        }.toMap
      }
    }
}

private object Storage {

  import InsightEntry.{ BSONFields => F }

  def selectId(id: String)     = BSONDocument(F.id -> id)
  def selectUserId(id: String) = BSONDocument(F.userId -> id)
  val sortChronological        = BSONDocument(F.date -> 1)
  val sortAntiChronological    = BSONDocument(F.date -> -1)

  def combineDocs(docs: List[BSONDocument]) =
    docs.foldLeft(BSONDocument()) { case (acc, doc) =>
      acc ++ doc
    }
}
