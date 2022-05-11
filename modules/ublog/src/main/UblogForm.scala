package lila.ublog

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ cleanNonEmptyText, cleanText, stringIn, toMarkdown }
import lila.i18n.{ defaultLang, LangList }
import lila.user.User
import play.api.i18n.Lang
import lila.common.Markdown

final class UblogForm(markup: UblogMarkup, val captcher: lila.hub.actors.Captcher)(implicit
    ec: scala.concurrent.ExecutionContext
) extends lila.hub.CaptchedForm {

  import UblogForm._

  private val base =
    mapping(
      "title"       -> cleanNonEmptyText(minLength = 3, maxLength = 80),
      "intro"       -> cleanNonEmptyText(minLength = 0, maxLength = 1_000),
      "markdown"    -> toMarkdown(cleanNonEmptyText(minLength = 0, maxLength = 100_000)),
      "imageAlt"    -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "imageCredit" -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "language"    -> optional(stringIn(LangList.popularNoRegion.map(_.code).toSet)),
      "topics"      -> optional(text),
      "live"        -> boolean,
      "discuss"     -> boolean,
      "gameId"      -> text,
      "move"        -> text
    )(UblogPostData.apply)(UblogPostData.unapply)

  val create = Form(
    base.verifying(captchaFailMessage, validateCaptcha _)
  )

  def edit(post: UblogPost) =
    Form(base).fill(
      UblogPostData(
        title = post.title,
        intro = post.intro,
        markdown = removeLatex(post.markdown),
        imageAlt = post.image.flatMap(_.alt),
        imageCredit = post.image.flatMap(_.credit),
        language = post.language.code.some,
        topics = post.topics.map(_.value).mkString(", ").some,
        live = post.live,
        discuss = ~post.discuss,
        gameId = "",
        move = ""
      )
    )

  // $$something$$ breaks the TUI editor WYSIWYG
  private val latexRegex                      = s"""\\$${2,}+ *([^\\$$]+) *\\$${2,}+""".r
  private def removeLatex(markdown: Markdown) = markdown(m => latexRegex.replaceAllIn(m, """\$\$ $1 \$\$"""))
}

object UblogForm {

  case class UblogPostData(
      title: String,
      intro: String,
      markdown: Markdown,
      imageAlt: Option[String],
      imageCredit: Option[String],
      language: Option[String],
      topics: Option[String],
      live: Boolean,
      discuss: Boolean,
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
        language = LangList.removeRegion(realLanguage.orElse(user.realLang) | defaultLang),
        topics = topics ?? UblogTopic.fromStrList,
        image = none,
        live = false,
        discuss = Option(false),
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
        image = prev.image.map { i =>
          i.copy(alt = imageAlt, credit = imageCredit)
        },
        language = LangList.removeRegion(realLanguage | prev.language),
        topics = topics ?? UblogTopic.fromStrList,
        live = live,
        discuss = Option(discuss),
        updated = UblogPost.Recorded(user.id, DateTime.now).some,
        lived = prev.lived orElse live.option(UblogPost.Recorded(user.id, DateTime.now))
      )
  }

  val tier = Form(single("tier" -> number(min = UblogBlog.Tier.HIDDEN, max = UblogBlog.Tier.BEST)))
}
