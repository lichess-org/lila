package lila.perfStat

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final class PerfStatStorage(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val UserIdBSONHandler       = stringAnyValHandler[UserId](_.value, UserId.apply)
  implicit private val RatingAtBSONHandler     = Macros.handler[RatingAt]
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

  def update(perfStat: PerfStat): Funit =
    coll.update.one($id(perfStat.id), perfStat).void

  def insert(perfStat: PerfStat): Funit =
    coll.insert.one(perfStat).void
}
