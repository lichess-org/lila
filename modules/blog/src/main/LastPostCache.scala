package lila.blog

import lila.memo.{ CacheApi, Syncache }
import scala.concurrent.Future

final class LastPostCache(
    api: BlogApi,
    notifier: Notifier,
    config: BlogConfig,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cache = cacheApi.sync[Boolean, List[MiniPost]](
    name = "blog.lastPost",
    initialCapacity = 1,
    compute = _ => fetch,
    default = _ => Nil,
    expireAfter = Syncache.ExpireAfterWrite(config.lastPostTtl),
    strategy = Syncache.NeverWait
  )

  private def fetch: Fu[List[MiniPost]] = {
    Future.sequence(
      List("ja-JP", "en-US") map { lang =>
        api.prismicApi flatMap { prismic =>
          api.recent(prismic, page = 1, lila.common.config.MaxPerPage(3), none, lang) map {
            _ ?? {
              _.currentPageResults.toList flatMap MiniPost.fromDocument(config.collection, lang=lang)
            }
          }
        }
      }
    )
  }.map { _.flatten } addEffect maybeNotifyLastPost

  private var lastNotifiedId = none[String]

  private def maybeNotifyLastPost(posts: List[MiniPost]): Unit =
    posts.headOption foreach { last =>
      if (lastNotifiedId.??(last.id !=)) notifier(last.id)
      lastNotifiedId = last.id.some
    }

  def apply: List[MiniPost] = cache sync true
}
