package lila.history

import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.common.config.CollName

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Builder,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cacheTtl = 30 minutes

  private lazy val coll = db(CollName("history3"))

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]
}
