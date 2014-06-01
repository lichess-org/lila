package lila.blog

import scala.concurrent.duration._

final class LastPostCache(api: BlogApi, ttl: Duration) {

  private val cache = lila.memo.MixedCache.single[Option[MiniPost]](
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, none, 1) map (_.results.headOption flatMap MiniPost.fromDocument)
    },
    timeToLive = ttl,
    default = none,
    awaitTime = 1.millisecond)

  def apply = cache get true

  private[blog] def clear {
    cache invalidate true
  }
}
