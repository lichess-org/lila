package lila.blog

import scala.concurrent.duration._

final class LastPostCache(
    api: BlogApi,
    ttl: FiniteDuration,
    collection: String)(implicit system: akka.actor.ActorSystem) {

  private val cache = new lila.memo.Syncache[Boolean, List[MiniPost]](
    name = "blog.lastPost",
    compute = _ => fetch,
    default = _ => Nil,
    timeToLive = ttl,
    awaitTime = 1.millisecond,
    logger = logger)

  private def fetch = {
    println("----------- fetching from prismic!")
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, none, 3) map {
        _ ?? {
          _.results.toList flatMap MiniPost.fromDocument(collection)
        }
      }
    }
  }

  def apply = cache get true

  private[blog] def clear {
    cache invalidate true
  }
}
