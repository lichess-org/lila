package lila.blog

import io.prismic.Document
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.bson._

import lila.notify.{ Notification, NotifyApi, NewBlogPost }
import lila.user.UserRepo

private[blog] final class Notifier(
    blogApi: BlogApi,
    notifyApi: NotifyApi) {

  def apply(prismicId: String): Funit =
    blogApi.prismicApi flatMap { prismicApi =>
      blogApi.one(prismicApi, none, prismicId) flatten
        s"No such document: $prismicId" flatMap doSend
    }

  private def doSend(post: Document): Funit = {
    val content = NewBlogPost(
      id = NewBlogPost.Id(post.id),
      slug = NewBlogPost.Slug(post.slug),
      title = NewBlogPost.Title(~post.getText("blog.title")))
    UserRepo.recentlySeenNotKidIdsCursor(DateTime.now minusWeeks 1)
      .enumerate(500 * 1000, stopOnError = true) &>
      Enumeratee.map {
        _.getAs[String]("_id") err "User without an id"
      } |>>>
      Iteratee.foldM[String, Int](0) {
        case (count, userId) => notifyApi.addNotificationWithoutSkipOrEvent(
          Notification(Notification.Notifies(userId), content)
        ) inject (count + 1)
      } addEffect { count =>
        logger.info(s"Sent $count notifications")
      } void
  }
}
