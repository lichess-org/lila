package lila.blog

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    scheduler: lila.common.Scheduler,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifyApi: lila.notify.NotifyApi
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

  lazy val lastPostCache = new LastPostCache(api, LastPostCacheTtl, PrismicCollection)

  private lazy val notifier = new Notifier(
    blogApi = api,
    notifyApi = notifyApi
  )

  def cli = new lila.common.Cli {
    def process = {
      case "blog" :: "notify" :: prismicId :: Nil =>
        notifier(prismicId) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "blog" boot new Env(
    config = lila.common.PlayApp loadConfig "blog",
    scheduler = lila.common.PlayApp.scheduler,
    asyncCache = lila.memo.Env.current.asyncCache,
    notifyApi = lila.notify.Env.current.api
  )(
    lila.common.PlayApp.system
  )
}
