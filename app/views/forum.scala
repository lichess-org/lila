package views.forum

import play.api.data.Form
import play.api.libs.json.Json
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.forum.{ CategView, TopicView }
import lila.core.captcha.Captcha

lazy val bits = lila.forum.ui.ForumBits(helpers)(assetUrl)
lazy val post = lila.forum.ui.PostUi(helpers, bits)

object categ:
  private lazy val ui = lila.forum.ui.CategUi(helpers, bits)

  def index(categs: List[CategView])(using PageContext) =
    views.base.layout(
      title = trans.site.forum.txt(),
      moreCss = cssTag("forum"),
      csp = defaultCsp.withInlineIconFont.some,
      openGraph = lila.web
        .OpenGraph(
          title = "Lichess community forum",
          url = s"$netBaseUrl${routes.ForumCateg.index.url}",
          description = "Chess discussions and feedback about Lichess development"
        )
        .some
    )(ui.index(categs))

  def show(
      categ: lila.forum.ForumCateg,
      topics: Paginator[TopicView],
      canWrite: Boolean,
      stickyPosts: List[TopicView]
  )(using PageContext) =
    views.base.layout(
      title = categ.name,
      moreCss = cssTag("forum"),
      modules = infiniteScrollTag,
      csp = defaultCsp.withInlineIconFont.some,
      openGraph = lila.web
        .OpenGraph(
          title = s"Forum: ${categ.name}",
          url = s"$netBaseUrl${routes.ForumCateg.show(categ.slug).url}",
          description = categ.desc
        )
        .some
    )(ui.show(categ, topics, canWrite, stickyPosts))

object topic:
  private lazy val ui =
    lila.forum.ui.TopicUi(helpers, bits, post)(
      views.base.captcha.apply,
      lila.msg.MsgPreset.forumDeletion.presets
    )

  def form(categ: lila.forum.ForumCateg, form: Form[?], captcha: Captcha)(using PageContext) =
    views.base.layout(
      title = "New forum topic",
      moreCss = cssTag("forum"),
      modules = jsModule("bits.forum") ++ captchaTag
    )(ui.form(categ, form, captcha))

  def show(
      categ: lila.forum.ForumCateg,
      topic: lila.forum.ForumTopic,
      posts: Paginator[lila.forum.ForumPost.WithFrag],
      formWithCaptcha: Option[(Form[?], Captcha)],
      unsub: Option[Boolean],
      canModCateg: Boolean,
      formText: Option[String] = None,
      replyBlocked: Boolean = false
  )(using ctx: PageContext) =
    views.base.layout(
      title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
      modules = jsModule("bits.forum") ++ jsModule("bits.expandText") ++
        formWithCaptcha.isDefined.so(captchaTag),
      moreCss = cssTag("forum"),
      openGraph = lila.web
        .OpenGraph(
          title = topic.name,
          url = s"$netBaseUrl${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage).url}",
          description = shorten(posts.currentPageResults.headOption.so(_.post.text), 152)
        )
        .some,
      csp = defaultCsp.withInlineIconFont.withTwitter.some
    )(ui.show(categ, topic, posts, formWithCaptcha, unsub, canModCateg, formText, replyBlocked))

  def makeDiagnostic(categ: lila.forum.ForumCateg, form: Form[?], captcha: Captcha, text: String)(using
      PageContext
  )(using me: Me) =
    views.base.layout(
      title = "Diagnostic report",
      moreCss = cssTag("forum"),
      modules = jsModule("bits.forum")
        ++ jsModuleInit("bits.autoform", Json.obj("selector" -> ".post-text-area", "ops" -> "focus begin"))
        ++ captchaTag
    )(ui.makeDiagnostic(categ, form, captcha, text))

def search(text: String, pager: Paginator[lila.forum.PostView.WithReadPerm])(using PageContext) =
  val title = s"""${trans.search.search.txt()} "${text.trim}""""
  views.base.layout(
    title = title,
    modules = infiniteScrollTag,
    moreCss = cssTag("forum")
  )(post.search(text, pager, title))
