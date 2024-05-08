package lila.ublog
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.i18n.Language

final class UblogUi(helpers: Helpers, atomUi: AtomUi)(picfitUrl: lila.core.misc.PicfitUrl):
  import helpers.{ *, given }

  def thumbnail(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
    img(
      cls     := "ublog-post-image",
      widthA  := size(UblogPost.thumbnail).width,
      heightA := size(UblogPost.thumbnail).height,
      alt     := post.image.flatMap(_.alt)
    )(src := thumbnailUrl(post, size))

  def thumbnailUrl(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
    post.image match
      case Some(image) => UblogPost.thumbnail(picfitUrl, image.id, size)
      case _           => assetUrl("images/user-blog-default.png")

  enum ShowAt:
    case top, bottom, none

  def card(
      post: UblogPost.BasePost,
      makeUrl: UblogPost.BasePost => Call = urlOfPost,
      showAuthor: ShowAt = ShowAt.none,
      showIntro: Boolean = true
  )(using Context) =
    a(cls := "ublog-post-card ublog-post-card--link", href := makeUrl(post))(
      span(cls := "ublog-post-card__top")(
        thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
        post.lived.map { live => semanticDate(live.at)(cls := "ublog-post-card__over-image") },
        showAuthor match
          case ShowAt.none => emptyFrag
          case showAt =>
            userIdSpanMini(post.created.by)(cls := s"ublog-post-card__over-image pos-$showAt")
      ),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        showIntro.option(span(cls := "ublog-post-card__intro")(shorten(post.intro, 100)))
      )
    )

  def miniCard(post: UblogPost.BasePost) =
    span(cls := "ublog-post-card ublog-post-card--mini")(
      thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
      h3(cls := "ublog-post-card__title")(post.title)
    )

  def urlOfPost(post: UblogPost.BasePost) = post.blog match
    case UblogBlog.Id.User(userId) =>
      routes.Ublog.post(usernameOrId(userId), post.slug, post.id)

  def editUrlOfPost(post: UblogPost.BasePost) = routes.Ublog.edit(post.id)

  def newPostLink(user: User)(using Context) = a(
    href     := routes.Ublog.form(user.username),
    cls      := "button button-green",
    dataIcon := Icon.PlusButton,
    title    := trans.ublog.newPost.txt()
  )

  def blogPage(user: User, blog: UblogBlog, posts: Paginator[UblogPost.PreviewPost])(using ctx: Context) =
    val title = trans.ublog.xBlog.txt(user.username)
    Page(title)
      .css("ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit) ++ ctx.isAuth.so(EsmInit("bits.ublog")))
      .copy(atomLinkTag = link(href := routes.Ublog.userAtom(user.username), st.title := title).some)
      .robots(blog.listed):
        main(cls := "page-menu")(
          menu(Left(user.id)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(trans.ublog.xBlog(userLink(user))),
              div(cls := "box__top__actions")(
                blog.allows.moderate.option(tierForm(blog)),
                blog.allows.create.option(
                  frag(
                    a(href := routes.Ublog.drafts(user.username))(trans.ublog.drafts()),
                    newPostLink(user)
                  )
                ),
                atomUi.atomLink(routes.Ublog.userAtom(user.username))
              )
            ),
            standardFlash,
            if posts.nbResults > 0 then
              div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map(card(_)),
                pagerNext(posts, np => s"${routes.Ublog.index(user.username, np).url}")
              )
            else
              div(cls := "ublog-index__posts--empty")(
                trans.ublog.noPostsInThisBlogYet()
              )
          )
        )

  def community(
      language: Option[Language],
      posts: Paginator[UblogPost.PreviewPost],
      langSelections: List[(Language, String)]
  )(using ctx: Context) =
    def languageOrAll = language | Language("all")
    Page("Community blogs")
      .css("ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit))
      .copy(
        atomLinkTag = link(
          href     := routes.Ublog.communityAtom(languageOrAll),
          st.title := "Lichess community blogs"
        ).some
      )
      .hrefLangs(lila.ui.LangPath(langHref(routes.Ublog.communityAll()))):
        main(cls := "page-menu")(
          menu(Right("community")),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(trans.ublog.communityBlogs()),
              div(cls := "box__top__actions")(
                lila.ui.bits.mselect(
                  "ublog-lang",
                  language.fold("All languages")(langList.nameByLanguage),
                  langSelections
                    .map: (languageSel, name) =>
                      a(
                        href := {
                          if languageSel == Language("all") then routes.Ublog.communityAll()
                          else routes.Ublog.communityLang(languageSel)
                        },
                        cls := (languageSel == languageOrAll).option("current")
                      )(name)
                ),
                atomUi.atomLink(routes.Ublog.communityAtom(languageOrAll))
              )
            ),
            if posts.nbResults > 0 then
              div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map { card(_, showAuthor = ShowAt.top) },
                pagerNext(
                  posts,
                  p =>
                    language
                      .fold(routes.Ublog.communityAll(p))(l => routes.Ublog.communityLang(l, p))
                      .url
                )
              )
            else div(cls := "ublog-index__posts--empty")("Nothing to show.")
          )
        )

  def drafts(user: User, posts: Paginator[UblogPost.PreviewPost])(using Context) =
    Page(trans.ublog.drafts.txt())
      .css("ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit)):
        main(cls := "page-menu")(
          menu(Left(user.id)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(trans.ublog.drafts()),
              div(cls := "box__top__actions")(
                a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
                newPostLink(user)
              )
            ),
            if posts.nbResults > 0 then
              div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map { card(_, editUrlOfPost) },
                pagerNext(posts, np => routes.Ublog.drafts(user.username, np).url)
              )
            else
              div(cls := "ublog-index__posts--empty"):
                trans.ublog.noDrafts()
          )
        )

  def friends(posts: Paginator[UblogPost.PreviewPost])(using Context) = list(
    title = "Friends blogs",
    posts = posts,
    menuItem = "friends",
    route = (p, _) => routes.Ublog.friends(p),
    onEmpty = "Nothing to show. Follow some authors!"
  )

  def liked(posts: Paginator[UblogPost.PreviewPost])(using Context) = list(
    title = "Liked blog posts",
    posts = posts,
    menuItem = "liked",
    route = (p, _) => routes.Ublog.liked(p),
    onEmpty = "Nothing to show. Like some posts!"
  )

  def topic(top: UblogTopic, posts: Paginator[UblogPost.PreviewPost], byDate: Boolean)(using Context) =
    list(
      title = s"Blog posts about $top",
      posts = posts,
      menuItem = "topics",
      route = (p, bd) => routes.Ublog.topic(top.value, p, ~bd),
      onEmpty = "Nothing to show.",
      byDate.some
    )

  private def list(
      title: String,
      posts: Paginator[UblogPost.PreviewPost],
      menuItem: String,
      route: (Int, Option[Boolean]) => Call,
      onEmpty: => Frag,
      byDate: Option[Boolean] = None
  )(using Context) =
    Page(title)
      .css("ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit)):
        main(cls := "page-menu")(
          menu(Right(menuItem)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(title),
              byDate.map: v =>
                span(
                  "Sort by ",
                  span(cls := "btn-rack")(
                    a(cls := s"btn-rack__btn${(!v).so(" active")}", href := route(1, false.some))("rank"),
                    a(cls := s"btn-rack__btn${v.so(" active")}", href := route(1, true.some))("date")
                  )
                )
            ),
            if posts.nbResults > 0 then
              div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map { card(_, showAuthor = ShowAt.top) },
                pagerNext(posts, np => route(np, byDate).url)
              )
            else div(cls := "ublog-index__posts--empty")(onEmpty)
          )
        )

  def topics(tops: List[UblogTopic.WithPosts])(using Context) =
    Page("All blog topics").css("ublog"):
      main(cls := "page-menu")(
        menu(Right("topics")),
        div(cls := "page-menu__content box")(
          boxTop(h1(trans.ublog.blogTopics())),
          div(cls := "ublog-topics")(
            tops.map { case UblogTopic.WithPosts(topic, posts, nb) =>
              a(cls := "ublog-topics__topic", href := routes.Ublog.topic(topic.url))(
                h2(
                  topic.value,
                  span(cls := "ublog-topics__topic__nb")(trans.ublog.viewAllNbPosts(nb), " »")
                ),
                span(cls := "ublog-topics__topic__posts ublog-post-cards")(
                  posts.map(miniCard)
                )
              )
            }
          )
        )
      )

  def urlOfBlog(blog: UblogBlog): Call = urlOfBlog(blog.id)
  def urlOfBlog(blogId: UblogBlog.Id): Call = blogId match
    case UblogBlog.Id.User(userId) => routes.Ublog.index(usernameOrId(userId))

  private def tierForm(blog: UblogBlog) = postForm(action := routes.Ublog.setTier(blog.id.full)) {
    val form = lila.ublog.UblogForm.tier.fill(blog.tier)
    frag(
      span(dataIcon := Icon.Agent, cls := "text")("Set to:"),
      form3.select(form("tier"), lila.ublog.UblogRank.Tier.options)
    )
  }

  def menu(active: Either[UserId, String])(using ctx: Context) =
    def isRight(s: String) = active.fold(_ => false, _ == s)
    val lichess            = active.left.toOption.has(UserId.lichess)
    val community = active == Right("community") || (active.left.toOption.exists(ctx.isnt) && !lichess)
    val mine      = active.left.toOption.exists(ctx.is)
    lila.ui.bits.pageMenuSubnav(
      cls := "force-ltr",
      ctx.kid.no.option(
        a(
          cls  := community.option("active"),
          href := langHref(routes.Ublog.communityAll())
        )(trans.ublog.communityBlogs())
      ),
      ctx.kid.no.option(
        a(cls := isRight("topics").option("active"), href := routes.Ublog.topics)(
          trans.ublog.blogTopics()
        )
      ),
      (ctx.isAuth && ctx.kid.no).option(
        a(
          cls  := isRight("friends").option("active"),
          href := routes.Ublog.friends()
        )(trans.ublog.friendBlogs())
      ),
      ctx.kid.no.option(
        a(cls := isRight("liked").option("active"), href := routes.Ublog.liked())(
          trans.ublog.likedBlogs()
        )
      ),
      ctx.me
        .ifTrue(ctx.kid.no)
        .map: me =>
          a(cls := mine.option("active"), href := routes.Ublog.index(me.username))("My blog"),
      a(cls := lichess.option("active"), href := routes.Ublog.index(UserName.lichess))("Lichess blog")
    )

  object atom:
    def user(
        user: User,
        posts: Seq[UblogPost.PreviewPost]
    )(using Translate) =
      atomUi.feed(
        elems = posts,
        htmlCall = routes.Ublog.index(user.username),
        atomCall = routes.Ublog.userAtom(user.username),
        title = trans.ublog.xBlog.txt(user.username),
        updated = posts.headOption.flatMap(_.lived).map(_.at)
      ): post =>
        renderPost(post, authorOfBlog(post.blog))

    def community(language: Language, posts: Seq[UblogPost.PreviewPost]) =
      atomUi.feed(
        elems = posts,
        htmlCall = routes.Ublog.communityLang(language),
        atomCall = routes.Ublog.communityAtom(language),
        title = "Lichess community blogs",
        updated = posts.headOption.flatMap(_.lived).map(_.at)
      ) { post =>
        renderPost(post, authorOfBlog(post.blog))
      }

    private def renderPost(post: UblogPost.PreviewPost, authorName: String) =
      frag(
        tag("id")(post.id),
        tag("published")(post.lived.map(_.at).map(atomUi.atomDate)),
        link(
          rel  := "alternate",
          tpe  := "text/html",
          href := s"$netBaseUrl${urlOfPost(post)}"
        ),
        tag("title")(post.title),
        post.topics.map { topic =>
          atomUi.category(
            term = topic.url,
            label = topic.value,
            scheme = s"$netBaseUrl${routes.Ublog.topic(topic.url)}".some
          )
        },
        tag("content")(tpe := "html")(
          thumbnail(post, _.Size.Large),
          "<br>", // yes, scalatags encodes it.
          post.intro
        ),
        tag("tag")("media:thumbnail")(attr("url") := thumbnailUrl(post, _.Size.Large)),
        tag("author")(tag("name")(authorName))
      )

    private def authorOfBlog(blogId: UblogBlog.Id): String = blogId match
      case UblogBlog.Id.User(userId) => titleNameOrId(userId)
