package lila.ublog
package ui

import java.time.YearMonth
import java.time.temporal.ChronoUnit.DAYS
import scalalib.paginator.Paginator
import scalalib.model.Language
import lila.ui.*
import lila.core.ublog.{ BlogsBy, QualityFilter }

import ScalatagsTemplate.{ *, given }

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
      showIntro: Boolean = true,
      strictDate: Boolean = true
  )(using Context) =
    a(
      cls  := s"ublog-post-card ublog-post-card--link ublog-post-card--by-${post.created.by}",
      href := makeUrl(post)
    )(
      span(cls := "ublog-post-card__top")(
        thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
        post.lived.map { live =>
          if strictDate || DAYS.between(live.at, nowInstant) < 30 then
            semanticDate(live.at)(cls := "ublog-post-card__over-image")
          else span(cls := "ublog-post-card__over-image")("Timeless")
        },
        if showAuthor != ShowAt.none
        then userIdSpanMini(post.created.by)(cls := s"ublog-post-card__over-image pos-$showAuthor")
        else if ~post.sticky
        then span(dataIcon := Icon.Star, cls := "user-link ublog-post-card__over-image pos-top")
        else emptyFrag
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
      .css("bits.ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit) ++ ctx.isAuth.so(Esm("bits.ublog")))
      .copy(atomLinkTag = link(href := routes.Ublog.userAtom(user.username), st.title := title).some)
      .flag(_.noRobots, !blog.listed):
        main(cls := "page-menu")(
          menu(Left(user.id)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(trans.ublog.xBlog(userLink(user, withFlair = false))),
              div(cls := "box__top__actions")(
                blog.allows.moderate.option(tierForm(blog)),
                blog.allows.draft.option(
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
      filter: QualityFilter,
      posts: Paginator[UblogPost.PreviewPost],
      langSelections: List[(Language, String)]
  )(using ctx: Context) =
    def languageOrAll = language | Language("all")
    Page("Community blogs")
      .css("bits.ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit))
      .copy(
        atomLinkTag = link(
          href     := routes.Ublog.communityAtom(languageOrAll),
          st.title := "Lichess community blogs"
        ).some
      )
      .hrefLangs(lila.ui.LangPath(langHref(routes.Ublog.communityAll(filter.some)))):
        main(cls := "page-menu")(
          menu(Right("community")),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(cls := "collapsible")("Recent posts"),
              div(cls := "box__top__actions")(
                filterAndSort(filter.some, none, (f, _) => routes.Ublog.communityLang(languageOrAll, f.some)),
                lila.ui.bits.mselect(
                  "ublog-lang",
                  language.fold(trans.site.allLanguages.txt())(langList.nameByLanguage),
                  langSelections
                    .map: (languageSel, name) =>
                      a(
                        href := {
                          if languageSel == Language("all") then routes.Ublog.communityAll(filter.some)
                          else routes.Ublog.communityLang(languageSel, filter.some)
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
                      .fold(routes.Ublog.communityAll(filter.some, p)): l =>
                        routes.Ublog.communityLang(l, filter.some, p)
                      .url
                )
              )
            else div(cls := "ublog-index__posts--empty")("Nothing to show.")
          )
        )

  def drafts(user: User, blog: UblogBlog, posts: Paginator[UblogPost.PreviewPost])(using ctx: Context) =
    Page(trans.ublog.drafts.txt())
      .css("bits.ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit)):
        main(cls := "page-menu")(
          menu(Left(user.id)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              h1(
                ctx.isnt(user).option(frag(userLink(user, withFlair = false), "'s ")),
                trans.ublog.drafts()
              ),
              div(cls := "box__top__actions")(
                a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
                newPostLink(user)
              )
            ),
            if posts.nbResults > 0 then
              val url = if blog.allows.edit then editUrlOfPost else urlOfPost
              div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map { card(_, url) },
                pagerNext(posts, np => routes.Ublog.drafts(user.username, np).url)
              )
            else div(cls := "ublog-index__posts--empty")(trans.ublog.noDrafts())
          )
        )

  def friends(posts: Paginator[UblogPost.PreviewPost])(using Context) = list(
    title = "Blog posts by friends",
    posts = posts,
    menuItem = "friends",
    route = (p, _, _) => routes.Ublog.friends(p),
    onEmpty = "Nothing to show. Follow some authors!"
  )

  def liked(posts: Paginator[UblogPost.PreviewPost])(using Context) = list(
    title = trans.ublog.likedBlogs.txt(),
    posts = posts,
    menuItem = "liked",
    route = (p, _, _) => routes.Ublog.liked(p),
    onEmpty = "Nothing to show. Like some posts!"
  )

  def topic(top: UblogTopic, filter: QualityFilter, by: BlogsBy, posts: Paginator[UblogPost.PreviewPost])(
      using Context
  ) =
    list(
      title = s"$top posts",
      posts = posts,
      menuItem = "topics",
      route = (p, f, b) => routes.Ublog.topic(top.value, f.some, b, p),
      onEmpty = "Nothing to show.",
      filterOpt = filter.some,
      byOpt = by.some
    )

  def month(
      yearMonth: YearMonth,
      filter: QualityFilter,
      by: BlogsBy,
      posts: Paginator[UblogPost.PreviewPost]
  )(using
      Context
  ) =
    list(
      title = s"$yearMonth posts",
      posts = posts,
      menuItem = "by-month",
      route = (p, f, b) => routes.Ublog.byMonth(yearMonth.getYear, yearMonth.getMonthValue, f.some, b, p),
      onEmpty = "Nothing to show.",
      filterOpt = filter.some,
      byOpt = by.some,
      header = boxTop(cls := "ublog-index__calendar")(
        h1(cls := "collapsible")(trans.ublog.byMonth()),
        lila.ui.bits.calendarMselect(
          helpers,
          "by-month",
          allYears = UblogByMonth.allYears,
          firstMonth = monthOfFirstPost,
          url = (y, m) => routes.Ublog.byMonth(y, m, filter.some, by)
        )(yearMonth),
        filterAndSort(
          filter.some,
          by.some,
          (f, b) => routes.Ublog.byMonth(yearMonth.getYear, yearMonth.getMonthValue, f.some, b, 1)
        )
      ).some
    )

  def search(
      text: String,
      by: BlogsBy,
      paginator: Option[Paginator[UblogPost.PreviewPost]] = none
  )(using Context) =
    import BlogsBy.*
    Page("Search")
      .css("bits.ublog")
      .js(paginator.exists(_.hasNextPage).option(infiniteScrollEsmInit)):
        main(cls := "page-menu")(
          menu(Right("search")),
          div(cls := "page-menu__content box box-pad ublog-index")(
            boxTop(
              form(action := routes.Ublog.search(), cls := "search", method := "get")(
                h1(cls := "collapsible")("Search"),
                span(cls := "search-input")(
                  input(name := "text", value := text, size := "8", enterkeyhint := "search"),
                  submitButton(cls := "button", name := "by", value := by.toString)(dataIcon := Icon.Search)
                ),
                span(cls := "search-sort")(
                  "Sort",
                  span(cls := "btn-rack")(
                    List(score, likes).map: btnBy =>
                      submitButton(btnCls(by == btnBy), name := "by", value := btnBy.name)(btnBy.name),
                    by match
                      case BlogsBy.newest =>
                        submitButton(btnCls(true, "descending"), name := "by", value := "oldest")("date")
                      case BlogsBy.oldest =>
                        submitButton(btnCls(true, "ascending"), name := "by", value := "newest")("date")
                      case _ =>
                        submitButton(btnCls(false, "descending"), name := "by", value := "newest")("date")
                  )
                )
              )
            ),
            paginator match
              case Some(pager) if pager.nbResults > 0 =>
                div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
                  pager.currentPageResults.map(card(_, showAuthor = ShowAt.top)),
                  pagerNext(pager, np => routes.Ublog.search(text, by, np).url)
                )
              case _ => div(cls := "ublog-index__posts--empty")("No results")
          )
        )

  def modShowCarousel(posts: UblogPost.CarouselPosts)(using Context) =
    Page("Blog carousel")
      .css("bits.ublog")
      .js(Esm("bits.ublog")):
        main(cls := "page-menu")(
          bits.modMenu("carousel"),
          div(cls := "page-menu__content box box-pad")(
            div(cls := "ublog-index__posts ublog-mod-carousel")(
              (posts.pinned ++ posts.queue).map: p =>
                val by = userIdLink(
                  p.featured.map(_.by),
                  withFlair = false,
                  withOnline = false,
                  withPowerTip = false
                )
                div(
                  span(
                    p.featured
                      .so(_.until)
                      .map(until => label("Pinned by ", by, s" until ${showDate(until)}")),
                    p.featured.so(_.at).map(at => label("Added by ", by, s" ${showDate(at)}")),
                    form(action := routes.Ublog.modPull(p.id), method := "POST")(
                      input(tpe := "submit", cls := "pull", value := Icon.X)
                    )
                  ),
                  card(p, showAuthor = ShowAt.top, showIntro = false)
                )
            )
          )
        )

  def topics(tops: List[UblogTopic.WithPosts])(using Context) =
    Page("All blog topics").css("bits.ublog"):
      main(cls := "page-menu")(
        menu(Right("topics")),
        div(cls := "page-menu__content box box-pad ublog-index")(
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

  def urlOfBlog(blog: UblogBlog): Call      = urlOfBlog(blog.id)
  def urlOfBlog(blogId: UblogBlog.Id): Call = blogId match
    case UblogBlog.Id.User(userId) => routes.Ublog.index(usernameOrId(userId))

  def menu(active: Either[UserId, String])(using ctx: Context) =
    def isRight(s: String)  = active.fold(_ => false, _ == s)
    def isActive(s: String) = isRight(s).option("active")
    val lichess             = active.left.toOption.has(UserId.lichess)
    val community = active == Right("community") || (active.left.toOption.exists(ctx.isnt) && !lichess)
    val mine      = active.left.toOption.exists(ctx.is)
    lila.ui.bits.pageMenuSubnav(
      cls := "force-ltr",
      ctx.kid.no.option(
        frag(
          a(
            cls  := community.option("active"),
            href := langHref(routes.Ublog.communityAll())
          )(trans.ublog.community()),
          a(
            cls  := isActive("search"),
            href := langHref(routes.Ublog.search())
          )("Search"),
          a(
            cls  := isActive("by-month"),
            href := langHref(routes.Ublog.thisMonth())
          )(trans.ublog.byMonth()),
          a(cls := isActive("topics"), href := routes.Ublog.topics)(
            trans.ublog.byTopic()
          )
        )
      ),
      a(cls := lichess.option("active"), href := routes.Ublog.index(UserName.lichess))(
        trans.ublog.byLichess()
      ),
      ctx.kid.no.option(
        frag(
          div(cls := "sep"),
          a(cls := isActive("liked"), href := routes.Ublog.liked())(
            trans.ublog.myLikes()
          ),
          ctx.me.map: me =>
            frag(
              a(
                cls  := isActive("friends"),
                href := routes.Ublog.friends()
              )(trans.ublog.myFriends()),
              a(cls := mine.option("active"), href := routes.Ublog.index(me.username))(trans.ublog.myBlog())
            )
        )
      )
    )

  private def list(
      title: String,
      posts: Paginator[UblogPost.PreviewPost],
      menuItem: String,
      route: (Int, QualityFilter, BlogsBy) => Call,
      onEmpty: => Frag,
      filterOpt: Option[QualityFilter] = none,
      byOpt: Option[BlogsBy] = none,
      header: Option[Frag] = none
  )(using ctx: Context) =
    val by = byOpt.getOrElse(BlogsBy.newest)
    Page(title)
      .css("bits.ublog")
      .js(posts.hasNextPage.option(infiniteScrollEsmInit)):
        main(cls := "page-menu")(
          menu(Right(menuItem)),
          div(cls := "page-menu__content box box-pad ublog-index")(
            header | boxTop(
              h1(cls := "collapsible")(title),
              filterAndSort(filterOpt, byOpt, (f, b) => route(1, f, b))
            ),
            if posts.nbResults > 0 && posts.currentPageResults.size > 0 then
              div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
                posts.currentPageResults.map { card(_, showAuthor = ShowAt.top) },
                pagerNext(posts, np => route(np, filterOpt.getOrElse(QualityFilter.all), by).url)
              )
            else div(cls := "ublog-index__posts--empty")(onEmpty)
          )
        )

  private def filterAndSort(
      filterOpt: Option[QualityFilter],
      sortOpt: Option[BlogsBy],
      route: (QualityFilter, BlogsBy) => Call
  )(using Context) =
    import BlogsBy.*
    val sort      = sortOpt | newest
    val filter    = filterOpt | QualityFilter.best
    val filterBtn = (f: QualityFilter) => a(btnCls(filter == f), href := route(f, sort))(f.name)
    div(cls := "filter-and-sort")(
      filterOpt.isDefined.option(
        span(
          "Show",
          if Granter.opt(_.ModerateBlog) then span(cls := "btn-rack")(QualityFilter.values.map(filterBtn))
          else span(cls := "btn-rack")(filterBtn(QualityFilter.best), filterBtn(QualityFilter.all))
        )
      ),
      sortOpt.map: by =>
        span(
          "Sort",
          span(cls := "btn-rack")(
            a(btnCls(by == likes), href := route(filter, likes))("likes"),
            by match
              case BlogsBy.newest =>
                a(btnCls(true, "descending"), href := route(filter, oldest))("date")
              case BlogsBy.oldest =>
                a(btnCls(true, "ascending"), href := route(filter, newest))("date")
              case _ =>
                a(btnCls(false, "descending"), href := route(filter, newest))("date")
          )
        )
    )

  private def btnCls(active: Boolean, other: String = ""): Modifier =
    cls := s"btn-rack__btn $other" + (if active then " lit" else "")

  private def tierForm(blog: UblogBlog) = postForm(action := routes.Ublog.setTier(blog.id.full)):
    val form = lila.ublog.UblogForm.tier.fill(blog.tier)
    frag(
      span(dataIcon := Icon.Agent, cls := "text")("Set to:"),
      form3.select(form("tier"), lila.ublog.UblogBlog.Tier.options)
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
        tag("id")(s"$netBaseUrl${urlOfPost(post)}"),
        tag("published")(post.lived.map(_.at).map(atomUi.atomDate)),
        tag("updated")(post.updated.orElse(post.lived).map(_.at).map(atomUi.atomDate)),
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
          frag(
            thumbnail(post, _.Size.Large),
            br,
            post.intro
          ).render // html as escaped string in xml
        ),
        tag("media:thumbnail")(attr("url") := thumbnailUrl(post, _.Size.Large)),
        tag("author")(tag("name")(authorName))
      )

    private def authorOfBlog(blogId: UblogBlog.Id): String = blogId match
      case UblogBlog.Id.User(userId) => titleNameOrId(userId)
