package lila.blog

import io.prismic.Document
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.bson._

import lila.message.{ ThreadRepo, Api => MessageApi }
import lila.user.UserRepo

private[blog] final class Notifier(
    blogApi: BlogApi,
    messageApi: MessageApi,
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
    ThreadRepo.reallyDeleteByCreatorId(lichessUserId) >> {
      val thread = makeThread(post)
      UserRepo.recentlySeenNotKidIdsCursor(DateTime.now minusWeeks 1)
        .enumerate(500 * 1000, stopOnError = true) &>
        Enumeratee.map {
          _.getAs[String]("_id") err "User without an id"
        } |>>>
        Iteratee.foldM[String, Int](0) {
          case (count, userId) =>
            messageApi.lichessThread(thread.copy(to = userId)) inject (count + 1)
        } addEffect { count =>
          logger.info(s"Sent $count messages")
        } void
    }

  private def makeThread(doc: Document) =
    lila.hub.actorApi.message.LichessThread(
      from = lichessUserId,
      to = "",
      subject = s"New blog post: ${~doc.getText("blog.title")}",
      message = s"""${~doc.getText("blog.shortlede")}

Continue reading this post on http://lichess.org/blog/${doc.id}/${doc.slug}""")
}
