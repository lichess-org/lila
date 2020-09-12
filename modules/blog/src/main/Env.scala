package lila.blog

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

private class BlogConfig(
    @ConfigName("prismic.api_url") val apiUrl: String,
    val collection: String,
    @ConfigName("last_post_cache.ttl") val lastPostTtl: FiniteDuration
)

@Module
final class Env(
    appConfig: Configuration,
    timelineApi: lila.timeline.EntryApi,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    ws: play.api.libs.ws.StandaloneWSClient
) {

  private val config = appConfig.get[BlogConfig]("blog")(AutoConfig.loader)

  lazy val api = wire[BlogApi]

  private lazy val notifier = wire[Notifier]

  lazy val lastPostCache = wire[LastPostCache]
}
