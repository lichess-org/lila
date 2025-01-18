package lila.perfStat

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final class PerfStatStorage(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val UserIdBSONHandler: BSONHandler[UserId]       = stringAnyValHandler[UserId](_.value, UserId.apply)
  implicit private val RatingAtBSONHandler: BSONDocumentHandler[RatingAt]     = Macros.handler[RatingAt]
  implicit private val ResultBSONHandler: BSONDocumentHandler[Result]       = Macros.handler[Result]
  implicit private val ResultsBSONHandler: BSONDocumentHandler[Results]      = Macros.handler[Results]
  implicit private val StreakBSONHandler: BSONDocumentHandler[Streak]       = Macros.handler[Streak]
  implicit private val StreaksBSONHandler: BSONDocumentHandler[Streaks]      = Macros.handler[Streaks]
  implicit private val PlayStreakBSONHandler: BSONDocumentHandler[PlayStreak]   = Macros.handler[PlayStreak]
  implicit private val ResultStreakBSONHandler: BSONDocumentHandler[ResultStreak] = Macros.handler[ResultStreak]
  implicit private val AvgBSONHandler: BSONDocumentHandler[Avg]          = Macros.handler[Avg]
  implicit private val CountBSONHandler: BSONDocumentHandler[Count]        = Macros.handler[Count]
  implicit private val PerfStatBSONHandler: BSONDocumentHandler[PerfStat]     = Macros.handler[PerfStat]

  def find(userId: String, perfType: PerfType): Fu[Option[PerfStat]] =
    coll.byId[PerfStat](PerfStat.makeId(userId, perfType))

  def update(perfStat: PerfStat): Funit =
    coll.update.one($id(perfStat.id), perfStat).void

  def insert(perfStat: PerfStat): Funit =
    coll.insert.one(perfStat).void
}
