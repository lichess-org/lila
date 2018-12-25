package lila.blog

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    asyncCache: lila.memo.AsyncCache.Builder,
    timelineApi: lila.timeline.EntryApi
)(implicit system: akka.actor.ActorSystem) {

  private val PrismicApiUrl = config getString "prismic.api_url"
  private val PrismicCollection = config getString "prismic.collection"
  private val LastPostCacheTtl = config duration "last_post_cache.ttl"

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
    config = lila.common.PlayApp loadConfig "blog",
    scheduler = lila.common.PlayApp.scheduler,
    asyncCache = lila.memo.Env.current.asyncCache,
    timelineApi = lila.timeline.Env.current.entryApi
  )(
    lila.common.PlayApp.system
  )
}
