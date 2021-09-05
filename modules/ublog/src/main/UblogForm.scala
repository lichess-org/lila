package lila.ublog

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ cleanNonEmptyText, cleanText, markdownImage }
import lila.user.User

final class UblogForm(markup: UblogMarkup) {

  import UblogForm._

  val create = Form(
    mapping(
      "title"    -> cleanNonEmptyText(minLength = 3, maxLength = 100),
      "intro"    -> cleanNonEmptyText(minLength = 0, maxLength = 2_000),
      "markdown" -> cleanNonEmptyText(minLength = 0, maxLength = 100_000).verifying(markdownImage.constraint),
      "live"     -> boolean
    )(UblogPostData.apply)(UblogPostData.unapply)
  )

  def edit(post: UblogPost) =
    create.fill(
      UblogPostData(title = post.title, intro = post.intro, markdown = post.markdown, live = post.live)
    )
}

object UblogForm {

  case class UblogPostData(title: String, intro: String, markdown: String, live: Boolean) {

    def create(user: User) = {
      val now = DateTime.now
      UblogPost(
        _id = UblogPost.Id(lila.common.ThreadLocalRandom nextString 8),
        user = user.id,
        title = title,
        intro = intro,
        markdown = markdown,
        image = none,
        live = false,
        createdAt = now,
        updatedAt = now,
        liveAt = live option now
      )
    }

    def update(prev: UblogPost) =
      prev.copy(
        title = title,
        intro = intro,
        markdown = markdown,
        live = live,
        liveAt = prev.liveAt orElse live.option(DateTime.now)
      )
  }
}
