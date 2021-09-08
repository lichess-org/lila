package lila.ublog

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ cleanNonEmptyText, cleanText, markdownImage, stringIn }
import lila.i18n.{ defaultLang, LangList }
import lila.user.User
import play.api.i18n.Lang

final class UblogForm(markup: UblogMarkup, val captcher: lila.hub.actors.Captcher)(implicit
    ec: scala.concurrent.ExecutionContext
) extends lila.hub.CaptchedForm {

  import UblogForm._

  private val base =
    mapping(
      "title"    -> cleanNonEmptyText(minLength = 3, maxLength = 80),
      "intro"    -> cleanNonEmptyText(minLength = 0, maxLength = 1_000),
      "markdown" -> cleanNonEmptyText(minLength = 0, maxLength = 100_000).verifying(markdownImage.constraint),
      "language" -> optional(stringIn(LangList.popularNoRegion.map(_.code).toSet)),
      "topics"   -> optional(text),
      "live"     -> boolean,
      "gameId"   -> text,
      "move"     -> text
    )(UblogPostData.apply)(UblogPostData.unapply)

  val create = Form(
    base.verifying(captchaFailMessage, validateCaptcha _)
  )

  def edit(post: UblogPost) =
    Form(base).fill(
      UblogPostData(
        title = post.title,
        intro = post.intro,
        markdown = post.markdown,
        language = post.language.code.some,
        topics = post.topics.map(_.value).mkString(", ").some,
        live = post.live,
        gameId = "",
        move = ""
      )
    )
}

object UblogForm {

  case class UblogPostData(
      title: String,
      intro: String,
      markdown: String,
      language: Option[String],
      topics: Option[String],
      live: Boolean,
      gameId: String,
      move: String
  ) {

    def realLanguage = language flatMap Lang.get

    def create(user: User) =
      UblogPost(
        _id = UblogPost.Id(lila.common.ThreadLocalRandom nextString 8),
        blog = UblogBlog.Id.User(user.id),
        title = title,
        intro = intro,
        markdown = markdown,
        language = realLanguage.orElse(user.realLang) | defaultLang,
        topics = topics ?? UblogPost.Topic.fromStrList,
        image = none,
        live = false,
        created = UblogPost.Recorded(user.id, DateTime.now),
        updated = none,
        lived = none,
        likes = UblogPost.Likes(1),
        views = UblogPost.Views(0)
      )

    def update(user: User, prev: UblogPost) =
      prev.copy(
        title = title,
        intro = intro,
        markdown = markdown,
        language = realLanguage | prev.language,
        topics = topics ?? UblogPost.Topic.fromStrList,
        live = live,
        updated = UblogPost.Recorded(user.id, DateTime.now).some,
        lived = prev.lived orElse live.option(UblogPost.Recorded(user.id, DateTime.now))
      )
  }

  val tier = Form(single("tier" -> number(min = UblogBlog.Tier.HIDDEN, max = UblogBlog.Tier.BEST)))
}
