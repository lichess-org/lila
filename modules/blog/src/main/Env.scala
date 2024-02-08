package lila.blog

import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration
import lila.common.config.CollName

private class BlogConfig(
    @ConfigName("prismic.api_url") val apiUrl: String,
    val collection: String,
    @ConfigName("last_post_cache.ttl") val lastPostTtl: FiniteDuration
)

@Module
final class Env(
    appConfig: Configuration,
    timelineApi: lila.timeline.EntryApi,
    cacheApi: lila.memo.CacheApi,
    baseUrl: lila.common.config.BaseUrl,
    db: lila.db.Db
)(using Executor, Scheduler, play.api.libs.ws.StandaloneWSClient, akka.stream.Materializer):

  private val config = appConfig.get[BlogConfig]("blog")(AutoConfig.loader)

  lazy val api = wire[BlogApi]

  private lazy val notifier = wire[Notifier]

  lazy val lastPostCache = wire[LastPostCache]

  private val feedColl   = db(CollName("daily_feed"))
  val dailyFeed          = wire[DailyFeed]
  val dailyFeedPaginator = wire[DailyFeedPaginatorBuilder]

  export dailyFeed.lastUpdate
