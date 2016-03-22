package lila.blog

import io.prismic.Document
import lila.message.{ ThreadRepo, Api => MessageApi }
import lila.user.UserRepo
import org.joda.time.DateTime

private[blog] final class Notifier(
    blogApi: BlogApi,
    messageApi: MessageApi,
    lastPostCache: LastPostCache,
    lichessUserId: String) {

  def sendMessages(prismicId: String): Funit =
    blogApi.prismicApi flatMap { prismicApi =>
      blogApi.one(prismicApi, none, prismicId) flatten
        s"No such document: $prismicId" flatMap { post =>
          ThreadRepo.visibleByUserContainingExists(user = lichessUserId, containing = post.id) flatMap {
            case true  => fufail("Messages already sent!")
            case false => doSend(post)
          }
        }
    }

  private def doSend(post: Document): Funit =
    UserRepo recentlySeenNotKidIds DateTime.now.minusWeeks(1) flatMap { userIds =>
      (ThreadRepo reallyDeleteByCreatorId lichessUserId) >> {
        val thread = makeThread(post)
        val futures = userIds.toStream map { userId =>
          messageApi.lichessThread(thread.copy(to = userId))
        }
        lila.common.Future.lazyFold(futures)(())((_, _) => ()) >>- lastPostCache.clear
      }
    }

  private def makeThread(doc: Document) =
    lila.hub.actorApi.message.LichessThread(
      from = lichessUserId,
      to = "",
      subject = s"New blog post: ${~doc.getText("blog.title")}",
      message = s"""${~doc.getText("blog.shortlede")}

Continue reading this post on http://lichess.org/blog/${doc.id}/${doc.slug}""")
}
