package lila.blog

import play.api.i18n.Lang

import lila.memo.{ CacheApi, Syncache }
import lila.hub.actorApi.timeline.BlogPost
import lila.timeline.EntryApi

final class LastPostCache(
    api: BlogApi,
    config: BlogConfig,
    timelineApi: EntryApi,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cache = cacheApi.sync[BlogLang.Code, List[MiniPost]](
    name = "blog.lastPost",
    initialCapacity = 2,
    compute = langCode => fetch(BlogLang.fromLangCode(langCode)),
    default = _ => Nil,
    expireAfter = Syncache.ExpireAfterWrite(config.lastPostTtl),
    strategy = Syncache.NeverWait
  )

  private def fetch(lang: BlogLang): Fu[List[MiniPost]] = {
    api.prismicApi flatMap { prismic =>
      api.recent(prismic, page = 1, lila.common.config.MaxPerPage(3), lang, none) map {
        _ ?? {
          _.currentPageResults.toList flatMap MiniPost.fromDocument(config.collection, "icon")
        }
      }
    }
  } addEffect maybeNotifyLastPost(lang)

  private var lastNotifiedId = none[String]

  // Blogs need to be published while server is up so the timeline entry can be inserted
  private def maybeNotifyLastPost(lang: BlogLang)(posts: List[MiniPost]): Unit =
    posts.headOption.filter(_ => lang == BlogLang.default) foreach { last =>
      if (lastNotifiedId.fold(false)(last.id !=)) timelineApi.broadcast.insert(BlogPost(last.id))
      lastNotifiedId = last.id.some
    }

  def apply(lang: BlogLang): List[MiniPost] = cache sync lang.code
  def apply(lang: Lang): List[MiniPost]     = apply(BlogLang.fromLang(lang))
}
