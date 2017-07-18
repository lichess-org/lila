package lila.perfStat

import reactivemongo.bson._

import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final class PerfStatStorage(coll: Coll) {

  private implicit val UserIdBSONHandler = stringAnyValHandler[UserId](_.value, UserId.apply)
  private implicit val RatingAtBSONHandler = Macros.handler[RatingAt]
  private implicit val ResultBSONHandler = Macros.handler[Result]
  private implicit val ResultsBSONHandler = Macros.handler[Results]
  private implicit val StreakBSONHandler = Macros.handler[Streak]
  private implicit val StreaksBSONHandler = Macros.handler[Streaks]
  private implicit val PlayStreakBSONHandler = Macros.handler[PlayStreak]
  private implicit val ResultStreakBSONHandler = Macros.handler[ResultStreak]
  private implicit val AvgBSONHandler = Macros.handler[Avg]
  private implicit val CountBSONHandler = Macros.handler[Count]
  private implicit val PerfStatBSONHandler = Macros.handler[PerfStat]

  def find(userId: String, perfType: PerfType): Fu[Option[PerfStat]] =
    coll.byId[PerfStat](PerfStat.makeId(userId, perfType))

  def update(perfStat: PerfStat): Funit =
    coll.update($id(perfStat.id), perfStat).void

  def insert(perfStat: PerfStat): Funit =
    coll.insert(perfStat).void
}
