package lila.blog

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

@Module
private class PlanConfig(
    @ConfigName("prismic.api_url") val apiUrl: String,
    @ConfigName("last_post_cache.ttl") val lastPostTtl: FiniteDuration
)

@Module
final class Env(
    appConfig: Configuration,
    asyncCache: lila.memo.AsyncCache.Builder,
    timelineApi: lila.timeline.EntryApi
)(implicit system: akka.actor.ActorSystem, ws: play.api.libs.ws.WSClient) {

  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  lazy val api = new BlogApi(
    prismicUrl = config.apiUrl,
    asyncCache = asyncCache,
    collection = "blog"
  )

  private lazy val notifier = wire[Notifier]

  lazy val lastPostCache = wire[LastPostCache]
}
