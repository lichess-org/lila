package lila.blog

import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration

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
    baseUrl: lila.common.config.BaseUrl
)(using Executor, Scheduler, play.api.libs.ws.StandaloneWSClient):

  private val config = appConfig.get[BlogConfig]("blog")(AutoConfig.loader)

  lazy val api = wire[BlogApi]

  private lazy val notifier = wire[Notifier]

  lazy val lastPostCache = wire[LastPostCache]
