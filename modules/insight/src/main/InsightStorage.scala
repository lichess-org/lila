package lila.insight

import chess.format.FEN
import reactivemongo.api.bson._

import lila.common.LilaOpening
import lila.db.AsyncColl
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType
import lila.user.User

final private class InsightStorage(val coll: AsyncColl)(implicit ec: scala.concurrent.ExecutionContext) {

  import InsightStorage._
  import BSONHandlers._
  import InsightEntry.{ BSONFields => F }

  def fetchFirst(userId: User.ID): Fu[Option[InsightEntry]] =
    coll(_.find(selectUserId(userId)).sort(sortChronological).one[InsightEntry])

  def fetchLast(userId: User.ID): Fu[Option[InsightEntry]] =
    coll(_.find(selectUserId(userId)).sort(sortAntiChronological).one[InsightEntry])

  def count(userId: User.ID): Fu[Int] =
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

  def removeAll(userId: User.ID) = coll(_.delete.one(selectUserId(userId)).void)

  def find(id: String) = coll(_.one[InsightEntry](selectId(id)))

  def openings(userId: User.ID): Fu[List[LilaOpening]] =
    coll {
      _.aggregateList(64) { framework =>
        import framework._
        Match(selectUserId(userId) ++ $doc(F.opening $exists true)) -> List(
          PipelineOperator($doc("$sortByCount" -> s"$$${F.opening}")),
          Limit(64)
        )
      }.map {
        _.flatMap {
          _.getAsOpt[LilaOpening]("_id")
        }
      }
    }

  def nbByPerf(userId: User.ID): Fu[Map[PerfType, Int]] =
    coll {
      _.aggregateList(PerfType.nonPuzzle.size) { framework =>
        import framework._
        Match($doc(F.userId -> userId)) -> List(
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

private object InsightStorage {

  import InsightEntry.{ BSONFields => F }

  def selectId(id: String)      = BSONDocument(F.id -> id)
  def selectUserId(id: User.ID) = BSONDocument(F.userId -> id)
  val sortChronological         = BSONDocument(F.date -> 1)
  val sortAntiChronological     = BSONDocument(F.date -> -1)

  def combineDocs(docs: List[BSONDocument]) =
    docs.foldLeft(BSONDocument()) { case (acc, doc) =>
      acc ++ doc
    }
}
