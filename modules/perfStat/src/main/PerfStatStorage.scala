package lila.perfStat

import reactivemongo.api.bson.*

import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType
import lila.rating.PerfType.GamePerf

final class PerfStatStorage(coll: AsyncCollFailingSilently)(using Executor):

  private given ratingAtHandler: BSONDocumentHandler[RatingAt] = Macros.handler
  private given BSONDocumentHandler[GameAt]                    = Macros.handler
  private given BSONDocumentHandler[Result]                    = Macros.handler
  private given resultsHandler: BSONDocumentHandler[Results]   = Macros.handler
  private given streakHandler: BSONDocumentHandler[Streak]     = Macros.handler
  private given BSONDocumentHandler[Streaks]                   = Macros.handler
  private given BSONDocumentHandler[PlayStreak]                = Macros.handler
  private given BSONDocumentHandler[ResultStreak]              = Macros.handler
  private given BSONDocumentHandler[Avg]                       = Macros.handler
  private given BSONDocumentHandler[Count]                     = Macros.handler
  private given BSONDocumentHandler[PerfStat]                  = Macros.handler

  def find(userId: UserId, perf: GamePerf): Fu[Option[PerfStat]] =
    coll(_.byId[PerfStat](PerfStat.makeId(userId, perf)))

  def insert(perfStat: PerfStat): Funit =
    coll(_.insert.one(perfStat).void)

  private[perfStat] def deleteAllFor(userId: UserId): Funit =
    coll(_.delete.one($doc("_id".$regex(s"^$userId/"))).void)

  def update(a: PerfStat, b: PerfStat): Funit = coll: c =>
    val sets = $doc(
      docDiff(a.count, b.count).mapKeys(k => s"count.$k").toList :::
        List(
          resultsDiff(a, b)(_.bestWins).map { set =>
            "bestWins" -> set
          },
          resultsDiff(a, b)(_.worstLosses).map { set =>
            "worstLosses" -> set
          },
          (a.worstLosses != b.worstLosses).so(resultsHandler.writeOpt(b.worstLosses)).map { worstLosses =>
            "worstLosses" -> worstLosses
          },
          streakDiff(a, b)(_.resultStreak.win.cur).map { set =>
            "resultStreak.win.cur" -> set
          },
          streakDiff(a, b)(_.resultStreak.win.max).map { set =>
            "resultStreak.win.max" -> set
          },
          streakDiff(a, b)(_.resultStreak.loss.cur).map { set =>
            "resultStreak.loss.cur" -> set
          },
          streakDiff(a, b)(_.resultStreak.loss.max).map { set =>
            "resultStreak.loss.max" -> set
          },
          ratingAtDiff(a, b)(_.highest).map { set =>
            "highest" -> set
          },
          ratingAtDiff(a, b)(_.lowest).map { set =>
            "lowest" -> set
          },
          streakHandler.writeOpt(b.playStreak.nb.cur).map { set =>
            "playStreak.nb.cur" -> set
          },
          streakDiff(a, b)(_.playStreak.nb.max).map { set =>
            "playStreak.nb.max" -> set
          },
          streakHandler.writeOpt(b.playStreak.time.cur).map { set =>
            "playStreak.time.cur" -> set
          },
          streakDiff(a, b)(_.playStreak.time.max).map { set =>
            "playStreak.time.max" -> set
          },
          b.playStreak.lastDate.flatMap(instantHandler.writeOpt).map { date =>
            "playStreak.lastDate" -> date
          }
        ).flatten
    )
    c.update
      .one($id(a.id), $doc("$set" -> sets))
      .void
  end update

  private def resultsDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Results): Option[Bdoc] =
    (getter(a) != getter(b)).so(resultsHandler.writeOpt(getter(b)))

  private def streakDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Streak): Option[Bdoc] =
    (getter(a) != getter(b)).so(streakHandler.writeOpt(getter(b)))

  private def ratingAtDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Option[RatingAt]): Option[Bdoc] =
    getter(b).so: r =>
      getter(a).forall(_ != r).so(ratingAtHandler.writeOpt(r))

  private def docDiff[T: BSONDocumentWriter](a: T, b: T): Map[String, BSONValue] =
    val (am, bm) = (docMap(a), docMap(b))
    bm.collect:
      case (field, v) if am.get(field).forall(_ != v) => field -> v

  private def docMap[T](a: T)(using writer: BSONDocumentWriter[T]) =
    writer.writeTry(a).get.toMap
