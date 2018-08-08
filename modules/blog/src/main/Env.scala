package lidraughts.blog

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: lidraughts.common.Scheduler,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    timelineApi: lidraughts.timeline.EntryApi
)(implicit system: akka.actor.ActorSystem) {

  private val PrismicApiUrl = config getString "prismic.api_url"
  private val PrismicCollection = config getString "prismic.collection"
  private val LastPostCacheTtl = config duration "last_post_cache.ttl"

  val RssEmail = config getString "rss.email"

  lazy val api = new BlogApi(
    prismicUrl = PrismicApiUrl,
    asyncCache = asyncCache,
    collection = PrismicCollection
  )

  private lazy val notifier = new Notifier(
    blogApi = api,
    timelineApi = timelineApi
  )

  lazy val lastPostCache = new LastPostCache(
    api,
    notifier,
    LastPostCacheTtl,
    PrismicCollection
  )
}

object Env {

  lazy val current: Env = "blog" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "blog",
    scheduler = lidraughts.common.PlayApp.scheduler,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    timelineApi = lidraughts.timeline.Env.current.entryApi
  )(
    lidraughts.common.PlayApp.system
  )
}
