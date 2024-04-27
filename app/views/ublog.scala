package views.ublog

import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.i18n.LangList
import lila.core.i18n.Language
import lila.ublog.{ UblogBlog, UblogTopic, UblogPost, UblogRank, UblogForm }
import lila.core.captcha.Captcha

def thumbnailUrl(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
  post.image match
    case Some(image) => UblogPost.thumbnail(picfitUrl, image.id, size)
    case _           => assetUrl("images/user-blog-default.png")

lazy val postUi = lila.ublog.ui.UblogPostUi(helpers)(
  ublogRank = env.ublog.rank,
  connectLinks = views.base.bits.connectLinks,
  thumbnailUrl = thumbnailUrl
)

lazy val ui = lila.ublog.ui.UblogUi(helpers, postUi, atomUi)

def post(
    user: User,
    blog: UblogBlog,
    post: UblogPost,
    markup: Frag,
    others: List[UblogPost.PreviewPost],
    liked: Boolean,
    followable: Boolean,
    followed: Boolean
)(using ctx: PageContext) =
  views.base.layout(
    moreCss = cssTag("ublog"),
    modules = jsModule("bits.expandText") ++ ctx.isAuth.so(jsModule("bits.ublog")),
    title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
    openGraph = lila.web
      .OpenGraph(
        `type` = "article",
        image = post.image.isDefined.option(thumbnailUrl(post, _.Size.Large)),
        title = post.title,
        url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id)}",
        description = post.intro
      )
      .some,
    atomLinkTag = link(
      href     := routes.Ublog.userAtom(user.username),
      st.title := trans.ublog.xBlog.txt(user.username)
    ).some,
    robots = netConfig.crawlable && blog.listed && (post.indexable || blog.tier >= UblogRank.Tier.HIGH),
    csp = defaultCsp.withTwitter.withInlineIconFont.some
  )(postUi.page(user, blog, post, markup, others, liked, followable, followed, menu = ui.menu(Left(user.id))))

def blog(user: User, blog: UblogBlog, posts: Paginator[UblogPost.PreviewPost])(using ctx: PageContext) =
  val title = trans.ublog.xBlog.txt(user.username)
  views.base.layout(
    moreCss = cssTag("ublog"),
    modules = posts.hasNextPage.option(infiniteScrollTag) ++ ctx.isAuth.so(jsModule("bits.ublog")),
    title = title,
    atomLinkTag = link(
      href     := routes.Ublog.userAtom(user.username),
      st.title := title
    ).some,
    robots = netConfig.crawlable && blog.listed
  )(ui.blogPage(user, blog, posts))

object form:
  import play.api.data.Form

  lazy val formUi = lila.ublog.ui.UblogFormUi(helpers, ui, postUi)(
    renderCaptcha = (form, captcha) =>
      _ ?=>
        captcha.fold(views.base.captcha.hiddenEmpty(form)): c =>
          views.base.captcha(form, c)
  )

  private def moreCss(using PageContext) = frag(cssTag("ublog.form"), cssTag("tagify"))

  def create(user: User, f: Form[UblogForm.UblogPostData], captcha: Captcha)(using PageContext) =
    views.base.layout(
      moreCss = moreCss,
      modules = jsModule("bits.ublogForm") ++ captchaTag,
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${trans.ublog.newPost.txt()}"
    )(formUi.create(user, f, captcha))

  def edit(post: UblogPost, f: Form[UblogForm.UblogPostData])(using ctx: PageContext) =
    views.base.layout(
      moreCss = moreCss,
      modules = jsModule("bits.ublogForm"),
      title = s"${trans.ublog.xBlog.txt(titleNameOrId(post.created.by))} • ${post.title}"
    )(formUi.edit(post, f))

object index:

  def drafts(user: User, posts: Paginator[UblogPost.PreviewPost])(using PageContext) =
    views.base.layout(
      moreCss = frag(cssTag("ublog")),
      modules = posts.hasNextPage.option(infiniteScrollTag),
      title = trans.ublog.drafts.txt()
    )(ui.drafts(user, posts))

  def friends(posts: Paginator[UblogPost.PreviewPost])(using PageContext) = list(
    title = "Friends blogs",
    posts = posts,
    menuItem = "friends",
    route = (p, _) => routes.Ublog.friends(p),
    onEmpty = "Nothing to show. Follow some authors!"
  )

  def liked(posts: Paginator[UblogPost.PreviewPost])(using PageContext) = list(
    title = "Liked blog posts",
    posts = posts,
    menuItem = "liked",
    route = (p, _) => routes.Ublog.liked(p),
    onEmpty = "Nothing to show. Like some posts!"
  )

  def topic(top: UblogTopic, posts: Paginator[UblogPost.PreviewPost], byDate: Boolean)(using PageContext) =
    list(
      title = s"Blog posts about $top",
      posts = posts,
      menuItem = "topics",
      route = (p, bd) => routes.Ublog.topic(top.value, p, ~bd),
      onEmpty = "Nothing to show.",
      byDate.some
    )

  def community(language: Option[Language], posts: Paginator[UblogPost.PreviewPost])(using ctx: PageContext) =
    views.base.layout(
      moreCss = cssTag("ublog"),
      modules = posts.hasNextPage.option(infiniteScrollTag),
      title = "Community blogs",
      atomLinkTag = link(
        href     := routes.Ublog.communityAtom(language.fold("all")(_.value)),
        st.title := "Lichess community blogs"
      ).some,
      withHrefLangs = lila.web.LangPath(langHref(routes.Ublog.communityAll())).some
    ):
      val langSelections: List[(String, String)] = ("all", "All languages") ::
        lila.i18n.LangPicker
          .sortFor(LangList.popularNoRegion, ctx.req)
          .map: l =>
            l.language -> LangList.name(l)
      ui.community(language, posts, langSelections)

  def topics(tops: List[UblogTopic.WithPosts])(using PageContext) =
    views.base.layout(
      moreCss = cssTag("ublog"),
      title = "All blog topics"
    )(ui.topics(tops))

  private def list(
      title: String,
      posts: Paginator[UblogPost.PreviewPost],
      menuItem: String,
      route: (Int, Option[Boolean]) => Call,
      onEmpty: => Frag,
      byDate: Option[Boolean] = None
  )(using PageContext) =
    views.base.layout(
      moreCss = cssTag("ublog"),
      modules = posts.hasNextPage.option(infiniteScrollTag),
      title = title
    )(ui.list(title, posts, menuItem, route, onEmpty, byDate))
