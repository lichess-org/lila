package lila.insight

import chess.format.FEN
import reactivemongo.api.bson._

import lila.common.{ LilaOpening, LilaOpeningFamily }
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

  private[insight] def openings(userId: User.ID): Fu[(List[LilaOpeningFamily], List[LilaOpening])] =
    coll {
      _.aggregateOne() { framework =>
        import framework._
        Match(selectUserId(userId) ++ $doc(F.opening $exists true)) -> List(
          Facet(
            List(
              "families" -> List(
                PipelineOperator($doc("$sortByCount" -> s"$$${F.openingFamily}")),
                Limit(24)
              ),
              "openings" -> List(PipelineOperator($doc("$sortByCount" -> s"$$${F.opening}")), Limit(64))
            )
          )
        )
      }.map2 { doc =>
        def readBest[A: BSONHandler](field: String): List[A] =
          (~doc.getAsOpt[List[Bdoc]](field)).flatMap(_.getAsOpt[A]("_id"))
        (readBest[LilaOpeningFamily]("families"), readBest[LilaOpening]("openings"))
      }
    }.map(_ | (Nil -> Nil))

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

object InsightStorage {

  import InsightEntry.{ BSONFields => F }

  def selectId(id: String)               = $doc(F.id -> id)
  def selectUserId(id: User.ID)          = $doc(F.userId -> id)
  def selectPeers(peers: Question.Peers) = $doc(F.rating $inRange peers.ratingRange)
  val sortChronological                  = $sort asc F.date
  val sortAntiChronological              = $sort desc F.date

  def combineDocs(docs: List[BSONDocument]) =
    docs.foldLeft(BSONDocument()) { case (acc, doc) =>
      acc ++ doc
    }
}
