package lila.ublog
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.i18n.Language

final class UblogUi(helpers: Helpers, postUi: UblogPostUi, atomUi: AtomUi):
  import helpers.{ *, given }

  def blogPage(user: User, blog: UblogBlog, posts: Paginator[UblogPost.PreviewPost])(using ctx: Context) =
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
                postUi.newPostLink(user)
              )
            ),
            atomUi.atomLink(routes.Ublog.userAtom(user.username))
          )
        ),
        standardFlash,
        if posts.nbResults > 0 then
          div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
            posts.currentPageResults.map { postUi.card(_) },
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
      langSelections: List[(String, String)]
  )(using ctx: Context) =
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
                      if languageSel == "all" then routes.Ublog.communityAll()
                      else routes.Ublog.communityLang(languageSel)
                    },
                    cls := (languageSel == language.fold("all")(_.value)).option("current")
                  )(name)
            ),
            atomUi.atomLink(routes.Ublog.communityAtom(language.fold("all")(_.value)))
          )
        ),
        if posts.nbResults > 0 then
          div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
            posts.currentPageResults.map { postUi.card(_, showAuthor = postUi.ShowAt.top) },
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
    main(cls := "page-menu")(
      menu(Left(user.id)),
      div(cls := "page-menu__content box box-pad ublog-index")(
        boxTop(
          h1(trans.ublog.drafts()),
          div(cls := "box__top__actions")(
            a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
            postUi.newPostLink(user)
          )
        ),
        if posts.nbResults > 0 then
          div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
            posts.currentPageResults.map { postUi.card(_, postUi.editUrlOfPost) },
            pagerNext(posts, np => routes.Ublog.drafts(user.username, np).url)
          )
        else
          div(cls := "ublog-index__posts--empty"):
            trans.ublog.noDrafts()
      )
    )

  def list(
      title: String,
      posts: Paginator[UblogPost.PreviewPost],
      menuItem: String,
      route: (Int, Option[Boolean]) => Call,
      onEmpty: => Frag,
      byDate: Option[Boolean] = None
  )(using Context) =
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
            posts.currentPageResults.map { postUi.card(_, showAuthor = postUi.ShowAt.top) },
            pagerNext(posts, np => route(np, byDate).url)
          )
        else div(cls := "ublog-index__posts--empty")(onEmpty)
      )
    )

  def topics(tops: List[UblogTopic.WithPosts])(using Context) =
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
                posts.map(postUi.miniCard)
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
      a(cls := lichess.option("active"), href := routes.Ublog.index("Lichess"))("Lichess blog")
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

    def community(code: String, posts: Seq[UblogPost.PreviewPost]) =
      atomUi.feed(
        elems = posts,
        htmlCall = routes.Ublog.communityLang(code),
        atomCall = routes.Ublog.communityAtom(code),
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
          href := s"$netBaseUrl${postUi.urlOfPost(post)}"
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
          postUi.thumbnail(post, _.Size.Large),
          "<br>", // yes, scalatags encodes it.
          post.intro
        ),
        tag("tag")("media:thumbnail")(attr("url") := postUi.thumbnailUrl(post, _.Size.Large)),
        tag("author")(tag("name")(authorName))
      )

    private def authorOfBlog(blogId: UblogBlog.Id): String = blogId match
      case UblogBlog.Id.User(userId) => titleNameOrId(userId)
