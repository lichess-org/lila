package lila.perfStat

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll
import lila.rating.PerfType

final class PerfStatApi(coll: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  implicit val PerfTypeBSONHandler = new BSONHandler[BSONInteger, PerfType] {
    def read(b: BSONInteger) = PerfType.byId get b.value err s"Invalid perf type id ${b.value}"
    def write(p: PerfType) = BSONInteger(p.id)
  }
  private implicit val RatingAtBSONHandler = Macros.handler[RatingAt]
  private implicit val ResultBSONHandler = Macros.handler[Result]
  private implicit val PlayStreakBSONHandler = Macros.handler[PlayStreak]
  private implicit val PerfStatBSONHandler = Macros.handler[PerfStat]
}
