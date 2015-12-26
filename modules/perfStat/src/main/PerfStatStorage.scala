package lila.perfStat

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll
import lila.rating.PerfType

final class PerfStatStorage(coll: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  implicit val PerfTypeBSONHandler = new BSONHandler[BSONInteger, PerfType] {
    def read(b: BSONInteger) = PerfType.byId get b.value err s"Invalid perf type id ${b.value}"
    def write(p: PerfType) = BSONInteger(p.id)
  }
  implicit val UserIdBSONHandler = new BSONHandler[BSONString, UserId] {
    def read(b: BSONString) = UserId(b.value)
    def write(u: UserId) = BSONString(u.value)
  }
  private implicit val RatingAtBSONHandler = Macros.handler[RatingAt]
  private implicit val ResultBSONHandler = Macros.handler[Result]
  private implicit val StreakBSONHandler = Macros.handler[Streak]
  private implicit val PlayStreakBSONHandler = Macros.handler[PlayStreak]
  private implicit val ResultStreakBSONHandler = Macros.handler[ResultStreak]
  private implicit val AvgBSONHandler = Macros.handler[Avg]
  private implicit val CountBSONHandler = Macros.handler[Count]
  private implicit val PerfStatBSONHandler = Macros.handler[PerfStat]

  def find(userId: String, perfType: PerfType): Fu[Option[PerfStat]] =
    coll.find(BSONDocument("_id" -> PerfStat.makeId(userId, perfType))).one[PerfStat]

  def update(perfStat: PerfStat): Funit =
    coll.update(BSONDocument("_id" -> perfStat.id), perfStat).void

  def insert(perfStat: PerfStat): Funit =
    coll.insert(perfStat).void
}
