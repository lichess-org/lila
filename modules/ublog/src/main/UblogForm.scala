package lila.ublog

import play.api.data.*
import play.api.data.Forms.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Form.{ cleanNonEmptyText, stringIn, into, given }
import lila.i18n.{ defaultLang, LangList }
import lila.user.User
import play.api.i18n.Lang

final class UblogForm(val captcher: lila.hub.actors.Captcher) extends lila.hub.CaptchedForm:

  import UblogForm.*

  private val base =
    mapping(
      "title"       -> cleanNonEmptyText(minLength = 3, maxLength = 80),
      "intro"       -> cleanNonEmptyText(minLength = 0, maxLength = 1_000),
      "markdown"    -> cleanNonEmptyText(minLength = 0, maxLength = 100_000).into[Markdown],
      "imageAlt"    -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "imageCredit" -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "language"    -> optional(stringIn(LangList.popularNoRegion.map(_.code).toSet)),
      "topics"      -> optional(text),
      "live"        -> boolean,
      "discuss"     -> boolean,
      "gameId"      -> of[GameId],
      "move"        -> text
    )(UblogPostData.apply)(unapply)

  val create = Form:
    base.verifying(captchaFailMessage, validateCaptcha)

  def edit(post: UblogPost) = Form(base).fill:
    UblogPostData(
      title = post.title,
      intro = post.intro,
      markdown = removeLatex(post.markdown),
      imageAlt = post.image.flatMap(_.alt),
      imageCredit = post.image.flatMap(_.credit),
      language = post.language.code.some,
      topics = post.topics.mkString(", ").some,
      live = post.live,
      discuss = ~post.discuss,
      gameId = GameId(""),
      move = ""
    )

  // $$something$$ breaks the TUI editor WYSIWYG
  private val latexRegex                      = """\${2,}+([^\$]++)\${2,}+""".r
  private def removeLatex(markdown: Markdown) = markdown.map(latexRegex.replaceAllIn(_, """\$\$ $1 \$\$"""))

object UblogForm:

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
      gameId: GameId,
      move: String
  ):

    def realLanguage = language flatMap Lang.get

    def create(user: User) =
      UblogPost(
        id = UblogPostId(ThreadLocalRandom nextString 8),
        blog = UblogBlog.Id.User(user.id),
        title = title,
        intro = intro,
        markdown = markdown,
        language = LangList.removeRegion(realLanguage.orElse(user.realLang) | defaultLang),
        topics = topics so UblogTopic.fromStrList,
        image = none,
        live = false,
        discuss = Option(false),
        created = UblogPost.Recorded(user.id, nowInstant),
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
        image = prev.image.map: i =>
          i.copy(alt = imageAlt, credit = imageCredit),
        language = LangList.removeRegion(realLanguage | prev.language),
        topics = topics so UblogTopic.fromStrList,
        live = live,
        discuss = Option(discuss),
        updated = UblogPost.Recorded(user.id, nowInstant).some,
        lived = prev.lived orElse live.option(UblogPost.Recorded(user.id, nowInstant))
      )

  val tier = Form:
    single:
      "tier" -> number(min = UblogBlog.Tier.HIDDEN.value, max = UblogBlog.Tier.BEST.value)
        .into[UblogBlog.Tier]
