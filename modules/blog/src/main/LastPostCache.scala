package lila.blog

import lila.common.BlogLangs
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
    expireAfter = Syncache.ExpireNever,
    strategy = Syncache.NeverWait,
    refreshAfter = Syncache.RefreshAfterWrite(config.lastPostTtl)
  )

  private def fetch: Fu[List[MiniPost]] = {
    val miniPosts = {
      Future.sequence(BlogLangs.langs.toList map { langCode =>
        api.prismicApi flatMap { prismic =>
          api.recent(prismic, page = 1, lila.common.config.MaxPerPage(3), none, langCode) map {
            _ ?? {
              _.currentPageResults.toList flatMap MiniPost.fromDocument(config.collection, langCode=langCode)
            }
          }
        }
      })
    } addEffect maybeNotifyLastPost map { _.flatten }
    miniPosts.map { m =>
      // println("fetched in lastpostcache length: " + m.length)
    }
    miniPosts
  }

  private val lastNotifiedId = collection.mutable.Map[String, Option[String]]().withDefaultValue(None)
  private val lastNotifiedTitle = collection.mutable.Map[String, Option[String]]().withDefaultValue(None)
  // stores whether the new english blog post was already translated
  private val wasTranslated = collection.mutable.Map[String, Option[Boolean]]().withDefaultValue(None)

  private def maybeNotifyLastPost(posts: List[List[MiniPost]]): Unit = {
    // println("lastNotifiedId: " + lastNotifiedId)
    // println("lastNotifiedTitle: " + lastNotifiedTitle)
    // println("wasTranslated" + wasTranslated)
    posts foreach { postsLang =>
      postsLang.headOption foreach { last =>
        // println("last in lastpostcache: " + last)
        val newBlogWasPosted = lastNotifiedId(last.langCode).??( (x) => {
          // println("lastNotifiedId(" + last.langCode + "): " + lastNotifiedId(last.langCode))
          // println("extracted id: " + x)
          last.id != x
        })
        // println("newBlogWasPosted: " + newBlogWasPosted)
        if (last.langCode == "en-US") {
          // if english, notify if new blog id was detected
          if (newBlogWasPosted) notifier(last.id, last.langCode)
        } else {
          val newEnBlogWasPosted = lastNotifiedId("en-US").??(last.id !=)
          if (newEnBlogWasPosted) {
            wasTranslated += (last.langCode -> Some(false))
          }
          // if japanese, notify if new blog title was detected and the japanese title is different from english title (meaning the blog was already translated)

          val enTitle = posts(BlogLangs.enIndex)(0).title
          // println("enTitle: " + enTitle)
          val titleDifferentFromEn = last.title != enTitle
          val titleSameWithEn = !titleDifferentFromEn
          if (titleSameWithEn) wasTranslated += (last.langCode -> Some(false))

          val titleWasChanged = lastNotifiedTitle(last.langCode).??(last.title !=)
          val notYetTranslated = !wasTranslated(last.langCode).getOrElse(false)
          // println(titleWasChanged, titleDifferentFromEn, notYetTranslated)

          if (titleWasChanged && titleDifferentFromEn && notYetTranslated) {
            notifier(last.id, last.langCode)
            wasTranslated += (last.langCode -> Some(true))
          }
        }
        lastNotifiedId += (last.langCode -> last.id.some)
        lastNotifiedTitle += (last.langCode -> last.title.some)
      }
    }
  }

  def apply: List[MiniPost] = {
    val miniPosts = cache sync true
    // println("miniPosts in lastpostcache length: " + miniPosts.length)
    miniPosts
  }
}
