package lila.perfStat

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final class PerfStatStorage(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val UserIdBSONHandler       = stringAnyValHandler[UserId](_.value, UserId.apply)
  implicit private val RatingAtBSONHandler     = Macros.handler[RatingAt]
  implicit private val GameAtBSONHandler       = Macros.handler[GameAt]
  implicit private val ResultBSONHandler       = Macros.handler[Result]
  implicit private val ResultsBSONHandler      = Macros.handler[Results]
  implicit private val StreakBSONHandler       = Macros.handler[Streak]
  implicit private val StreaksBSONHandler      = Macros.handler[Streaks]
  implicit private val PlayStreakBSONHandler   = Macros.handler[PlayStreak]
  implicit private val ResultStreakBSONHandler = Macros.handler[ResultStreak]
  implicit private val AvgBSONHandler          = Macros.handler[Avg]
  implicit private val CountBSONHandler        = Macros.handler[Count]
  implicit private val PerfStatBSONHandler     = Macros.handler[PerfStat]

  def find(userId: String, perfType: PerfType): Fu[Option[PerfStat]] =
    coll.byId[PerfStat](PerfStat.makeId(userId, perfType))

  def insert(perfStat: PerfStat): Funit =
    coll.insert.one(perfStat).void

  def update(a: PerfStat, b: PerfStat): Funit = {

    val sets = $doc(
      docDiff(a.count, b.count).mapKeys(k => s"count.$k").toList :::
        List(
          resultsDiff(a, b)(_.bestWins).map { set =>
            "bestWins" -> set
          },
          resultsDiff(a, b)(_.worstLosses).map { set =>
            "worstLosses" -> set
          },
          (a.worstLosses != b.worstLosses).??(ResultsBSONHandler.writeOpt(b.worstLosses)) map { worstLosses =>
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
          StreakBSONHandler.writeOpt(b.playStreak.nb.cur).map { set =>
            "playStreak.nb.cur" -> set
          },
          streakDiff(a, b)(_.playStreak.nb.max).map { set =>
            "playStreak.nb.max" -> set
          },
          StreakBSONHandler.writeOpt(b.playStreak.time.cur).map { set =>
            "playStreak.time.cur" -> set
          },
          streakDiff(a, b)(_.playStreak.time.max).map { set =>
            "playStreak.time.max" -> set
          },
          b.playStreak.lastDate.flatMap(BSONJodaDateTimeHandler.writeOpt).map { date =>
            "playStreak.lastDate" -> date
          }
        ).flatten
    )

    coll.update
      .one($id(a.id), $doc("$set" -> sets))
      .void
  }

  private def resultsDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Results): Option[Bdoc] =
    (getter(a) != getter(b)) ?? ResultsBSONHandler.writeOpt(getter(b))

  private def streakDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Streak): Option[Bdoc] =
    (getter(a) != getter(b)) ?? StreakBSONHandler.writeOpt(getter(b))

  private def ratingAtDiff(a: PerfStat, b: PerfStat)(getter: PerfStat => Option[RatingAt]): Option[Bdoc] =
    getter(b) ?? { r =>
      getter(a).fold(true)(_ != r) ?? RatingAtBSONHandler.writeOpt(r)
    }

  private def docDiff[T: BSONDocumentWriter](a: T, b: T): Map[String, BSONValue] = {
    val (am, bm) = (docMap(a), docMap(b))
    bm collect {
      case (field, v) if am.get(field).fold(true)(_ != v) => field -> v
    }
  }

  private def docMap[T](a: T)(implicit writer: BSONDocumentWriter[T]) =
    writer.writeTry(a).get.toMap
}
