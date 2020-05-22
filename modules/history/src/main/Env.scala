package lila.history

import com.softwaremill.macwire._

import lila.common.config.CollName

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val coll = db(CollName("history3"))

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]
}
