package lila.blog

import lila.message.{ ThreadRepo, Api => MessageApi }
import lila.user.UserRepo
import org.joda.time.DateTime

private[blog] final class Notifier(
    blogApi: BlogApi,
    messageApi: MessageApi,
    lastPostCache: LastPostCache,
    lichessUserId: String) {

  def apply {
    blogApi.prismicApi foreach { prismicApi =>
      blogApi.recent(prismicApi, none, 1) map (_.results.headOption) foreach {
        _ ?? { post =>
          ThreadRepo.visibleByUserContainingExists(user = lichessUserId, containing = post.id) foreach {
            case true => funit
            case false => UserRepo recentlySeenIds DateTime.now.minusWeeks(2) foreach { userIds =>
              (ThreadRepo reallyDeleteByCreatorId lichessUserId) >> {
                val thread = makeThread(post)
                val futures = userIds.toStream map { userId =>
                  messageApi.lichessThread(thread.copy(to = userId), lichessUserId)
                }
                lila.common.Future.lazyFold(futures)(())((_, _) => ()) >>- lastPostCache.clear
              }
            }
          }
        }
      }
    }
  }

  private def makeThread(doc: io.prismic.Document) =
    lila.hub.actorApi.message.LichessThread(
      to = "",
      subject = s"New blog post: ${~doc.getText("blog.title")}",
      message = s"""${~doc.getText("blog.shortlede")}

Continue reading this post on http://lichess.org/blog/${doc.id}/${doc.slug}""")
}
