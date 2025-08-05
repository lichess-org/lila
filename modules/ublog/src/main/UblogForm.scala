package lila.ublog

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import scalalib.model.Language

import lila.common.Form.{ cleanNonEmptyText, cleanText, into, given }
import lila.core.captcha.{ CaptchaApi, WithCaptcha }
import lila.core.i18n.{ LangList, toLanguage, defaultLanguage }
import lila.core.ublog.Quality

final class UblogForm(val captcher: CaptchaApi, langList: LangList):

  import UblogForm.UblogPostData

  private val base =
    mapping(
      "title" -> cleanNonEmptyText(minLength = 3, maxLength = 80),
      "intro" -> cleanNonEmptyText(minLength = 0, maxLength = 1_000),
      "markdown" -> cleanNonEmptyText(minLength = 0, maxLength = 100_000).into[Markdown],
      "imageAlt" -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "imageCredit" -> optional(cleanNonEmptyText(minLength = 3, maxLength = 200)),
      "language" -> optional(langList.popularLanguagesForm.mapping),
      "topics" -> optional(text),
      "live" -> boolean,
      "discuss" -> boolean,
      "sticky" -> boolean,
      "ads" -> boolean,
      "gameId" -> of[GameId],
      "move" -> text
    )(UblogPostData.apply)(unapply)

  val create = Form:
    base.verifying(lila.core.captcha.failMessage, captcher.validateSync)

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
      sticky = ~post.sticky,
      ads = ~post.ads,
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
      sticky: Boolean,
      ads: Boolean,
      gameId: GameId,
      move: String
  ) extends WithCaptcha:

    def create(user: User) =
      UblogPost(
        id = UblogPost.randomId,
        blog = UblogBlog.Id.User(user.id),
        title = title,
        intro = intro,
        markdown = markdown,
        language = language.orElse(user.realLang.map(toLanguage)) | defaultLanguage,
        topics = topics.so(UblogTopic.fromStrList),
        image = none,
        live = false,
        discuss = Option(false),
        sticky = Option(false),
        ads = Option(false),
        created = UblogPost.Recorded(user.id, nowInstant),
        updated = none,
        lived = none,
        featured = none,
        likes = UblogPost.Likes(1),
        views = UblogPost.Views(0),
        similar = none,
        automod = none
      )

    def update(user: User, prev: UblogPost) =
      prev.copy(
        title = title,
        intro = intro,
        markdown = markdown,
        image = prev.image.map: i =>
          i.copy(alt = imageAlt, credit = imageCredit),
        language = language | prev.language,
        topics = topics.so(UblogTopic.fromStrList),
        live = live,
        discuss = Option(discuss),
        sticky = Option(sticky),
        ads = Option(ads),
        updated = UblogPost.Recorded(user.id, nowInstant).some,
        lived = prev.lived.orElse(live.option(UblogPost.Recorded(user.id, nowInstant)))
      )

  lazy val modBlogForm = Form(
    tuple(
      "tier" -> number(min = UblogBlog.Tier.HIDDEN.value, max = UblogBlog.Tier.BEST.value)
        .into[UblogBlog.Tier],
      "note" -> cleanText(0, 800)
    )
  )

  case class ModPostData(
      quality: Option[Quality] = none,
      evergreen: Option[Boolean] = none,
      flagged: Option[String] = none,
      commercial: Option[String] = none,
      featured: Option[Boolean] = none,
      featuredUntil: Option[Int] = none
  ):

    def hasUpdates: Boolean =
      List(quality, evergreen, flagged, commercial, featured, featuredUntil).exists(_.isDefined)

    def text = List(
      quality.so(q => s"quality = $q"),
      evergreen.so(e => s"evergreen = $e"),
      flagged.so(f => "flagged = " + (if f == "" then "none" else s"\"$f\"")),
      commercial.so(c => "commercial = " + (if c == "" then "none" else s"\"$c\"")),
      featured.so(f => s"featured = $f"),
      featuredUntil.so(d => s"featured days = $d")
    ).flatten.mkString(", ")

    def diff(post: UblogPost): String =

      def diffString(label: String, optFrom: Option[String], to: String) =
        optFrom match
          case None => s"$label = \"$to\"".some
          case Some(from) if from == to => none
          case Some(from) => s"$label \"$from\" -> \"$to\"".some

      post.automod.fold(text): p =>
        List(
          evergreen.filter(_ != ~p.evergreen).map(e => s"evergreen = $e"),
          quality.flatMap(q => diffString("quality", p.quality.name.some, q.name)),
          flagged.flatMap(f => diffString("flagged", p.flagged, f)),
          commercial.flatMap(c => diffString("commercial", p.commercial, c)),
          featured.map: isFeatured =>
            if isFeatured then s"add to carousel" + featuredUntil.so(days => s" $days days")
            else "pull from carousel"
        ).flatten.mkString(", ")

  object ModPostData:
    given Reads[Quality] = Reads
      .of[Int]
      .map(Quality.fromOrdinal)
    def reads: Reads[ModPostData] =
      (
        (JsPath \ "quality")
          .readNullable[Quality]
          .and((JsPath \ "evergreen").readNullable[Boolean])
          .and((JsPath \ "flagged").readNullable[String].map(_.map(_.take(200))))
          .and((JsPath \ "commercial").readNullable[String].map(_.map(_.take(200))))
          .and((JsPath \ "featured").readNullable[Boolean])
          .and(
            (JsPath \ "featuredUntil")
              .readNullable[Int]
              .filter(JsonValidationError(s"bad featuredUntil"))(_.forall(d => d > 0 && d <= 31))
          )
      )(ModPostData.apply)
