package lila.blog

import lila.memo.{ CacheApi, Syncache }

final class LastPostCache(
    api: BlogApi,
    notifier: Notifier,
    config: BlogConfig,
    cacheApi: CacheApi
)(using Executor):

  private val cache = cacheApi.sync[Boolean, Option[MiniPost]](
    name = "blog.lastPost",
    initialCapacity = 1,
    compute = _ => fetch,
    default = _ => none,
    expireAfter = Syncache.ExpireAfter.Write(config.lastPostTtl),
    strategy = Syncache.Strategy.NeverWait
  )

  private def fetch: Fu[Option[MiniPost]] = {
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, page = 1, lila.common.config.MaxPerPage(2), none) mapz {
        _.currentPageResults.toList.map(_.copy(imgSize = "ublog")).flatMap(MiniPost.apply).headOption
      }
    }
  } addEffect maybeNotifyLastPost

  private var lastNotifiedId = none[String]

  private def maybeNotifyLastPost(post: Option[MiniPost]): Unit =
    post foreach { last =>
      if lastNotifiedId.so(last.id !=) then notifier(last.id)
      lastNotifiedId = last.id.some
    }

  def apply: Option[MiniPost] = cache sync true
