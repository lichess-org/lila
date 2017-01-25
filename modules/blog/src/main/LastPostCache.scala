package lila.blog

import scala.concurrent.duration._

import lila.memo.Syncache

final class LastPostCache(
    api: BlogApi,
    ttl: FiniteDuration,
    collection: String)(implicit system: akka.actor.ActorSystem) {

  private val cache = new Syncache[Boolean, List[MiniPost]](
    name = "blog.lastPost",
    compute = _ => fetch,
    default = _ => Nil,
    timeToLive = ttl,
    strategy = Syncache.NeverWait,
    logger = logger)

  private def fetch = {
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, none, 3) map {
        _ ?? {
          _.results.toList flatMap MiniPost.fromDocument(collection)
        }
      }
    }
  }

  def apply = cache sync true

  private[blog] def clear {
    cache invalidate true
  }
}
