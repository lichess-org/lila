package lila.blog

import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration
import lila.common.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    timelineApi: lila.timeline.EntryApi,
    cacheApi: lila.memo.CacheApi,
    baseUrl: lila.common.config.BaseUrl,
    db: lila.db.Db
)(using Executor, Scheduler):

  private val feedColl   = db(CollName("daily_feed"))
  val dailyFeed          = wire[DailyFeed]
  val dailyFeedPaginator = wire[DailyFeedPaginatorBuilder]

  export dailyFeed.lastUpdate
