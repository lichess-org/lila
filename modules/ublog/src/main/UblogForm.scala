package lila.ublog

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanNonEmptyText, stringIn, into, given }
import lila.i18n.{ defaultLanguage, LangList, Language, LangForm }
import lila.user.User

final class UblogForm(val captcher: lila.hub.actors.Captcher) extends lila.hub.CaptchedForm:

  import UblogForm.*

  private val base =
    mapping(
      "title"       -> cleanNonEmptyText(minLength = 3, maxLength = 80),
      "intro"       -> cleanNonEmptyText(minLength = 0, maxLength = 1_000),
      "markdown"    -> cleanNonEmptyText(minLength = 0, maxLength = 100_000).into[Markdown],
      "imageAlt"    -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "imageCredit" -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "language"    -> optional(LangForm.popularLanguages.mapping),
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
      markdown = lila.common.MarkdownToastUi.latex.removeFrom(post.markdown),
      imageAlt = post.image.flatMap(_.alt),
      imageCredit = post.image.flatMap(_.credit),
      language = post.language.some,
      topics = post.topics.mkString(", ").some,
      live = post.live,
      discuss = ~post.discuss,
      gameId = GameId(""),
      move = ""
    )

object UblogForm:

  case class UblogPostData(
      title: String,
      intro: String,
      markdown: Markdown,
      imageAlt: Option[String],
      imageCredit: Option[String],
      language: Option[Language],
      topics: Option[String],
      live: Boolean,
      discuss: Boolean,
      gameId: GameId,
      move: String
  ):

    def create(user: User) =
      UblogPost(
        id = UblogPost.randomId,
        blog = UblogBlog.Id.User(user.id),
        title = title,
        intro = intro,
        markdown = markdown,
        language = language.orElse(user.language) | defaultLanguage,
        topics = topics so UblogTopic.fromStrList,
        image = none,
        live = false,
        discuss = Option(false),
        created = UblogPost.Recorded(user.id, nowInstant),
        updated = none,
        lived = none,
        likes = UblogPost.Likes(1),
        views = UblogPost.Views(0),
        rankAdjustDays = none,
        pinned = none
      )

    def update(user: User, prev: UblogPost) =
      prev.copy(
        title = title,
        intro = intro,
        markdown = markdown,
        image = prev.image.map: i =>
          i.copy(alt = imageAlt, credit = imageCredit),
        language = language | prev.language,
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

  val adjust = Form:
    tuple(
      "days"   -> optional(number(min = -180, max = 180)),
      "pinned" -> boolean
    )
