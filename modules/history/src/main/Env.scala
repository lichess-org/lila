package lila.history

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName
import lila.rating.PerfType
import lila.core.user.WithPerf
import lila.core.rating.PerfKey
import lila.core.Days

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler, lila.core.i18n.Translator):

  private lazy val coll = db(CollName("history4")).failingSilently()

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]

  lazy val userHistoryApi = new lila.core.history.HistoryApi:
    def addPuzzle                                                                           = api.addPuzzle
    def progresses: (List[WithPerf], PerfKey, Days) => Future[List[(IntRating, IntRating)]] = api.progresses
