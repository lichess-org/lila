package lila.history

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.CollName

@Module
@annotation.nowarn("msg=unused")
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler):

  private lazy val coll = db(CollName("history4")).failingSilently()

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]
