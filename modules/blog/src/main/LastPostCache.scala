package lila.blog

import scala.concurrent.duration._

import lila.memo.Syncache

final class LastPostCache(
    api: BlogApi,
    notifier: Notifier,
    ttl: FiniteDuration,
    collection: String
)(implicit system: akka.actor.ActorSystem) {

  private val cache = new Syncache[Boolean, List[MiniPost]](
    name = "blog.lastPost",
    compute = _ => fetch,
    default = _ => Nil,
    expireAfter = Syncache.ExpireAfterWrite(ttl),
    strategy = Syncache.NeverWait,
    logger = logger
  )

  private def fetch: Fu[List[MiniPost]] = {
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, none, page = 1, lila.common.MaxPerPage(3)) map {
        _ ?? {
          _.currentPageResults.toList flatMap MiniPost.fromDocument(collection)
        }
      }
    }
  } addEffect maybeNotifyLastPost

  private var lastNotifiedId = none[String]

  private def maybeNotifyLastPost(posts: List[MiniPost]): Unit =
    posts.headOption foreach { last =>
      if (lastNotifiedId.??(last.id !=)) notifier(last.id)
      lastNotifiedId = last.id.some
    }

  def apply = cache sync true
}
