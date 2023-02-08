package lila.history

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.CollName
import akka.actor.ActorSystem

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.AsyncDb @@ lila.db.YoloDb
)(using
    ec: Executor,
    scheduler: Scheduler
):

  private lazy val coll = db(CollName("history4")).failingSilently()

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]
