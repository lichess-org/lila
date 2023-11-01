package views.html.ublog

import controllers.routes
import play.api.i18n.Lang
import play.api.mvc.Call

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.i18n.LangList
import lila.ublog.{ UblogPost, UblogTopic }
import lila.user.User

object index:

  import views.html.ublog.{ post as postView }

  def drafts(user: User, posts: Paginator[UblogPost.PreviewPost])(using PageContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = trans.ublog.drafts.txt()
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "mine".some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          boxTop(
            h1(trans.ublog.drafts()),
            div(cls := "box__top__actions")(
              a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
              postView.newPostLink
            )
          ),
          if posts.nbResults > 0 then
            div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, postView.editUrlOfPost) },
              pagerNext(posts, np => routes.Ublog.drafts(user.username, np).url)
            )
          else
            div(cls := "ublog-index__posts--empty")(
              trans.ublog.noDrafts()
            )
        )
      )
    }

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

  import views.html.ublog.post.ShowAt
  def community(lang: Option[Lang], posts: Paginator[UblogPost.PreviewPost])(using ctx: PageContext) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = "Community blogs",
      atomLinkTag = link(
        href     := routes.Ublog.communityAtom(lang.fold("all")(_.language)),
        st.title := "Lichess community blogs"
      ).some,
      withHrefLangs = lila.common.LangPath(langHref(routes.Ublog.communityAll())).some
    ) {
      val langSelections = ("all", "All languages") :: lila.i18n.I18nLangPicker
        .sortFor(LangList.popularNoRegion, ctx.req)
        .map { l =>
          l.language -> LangList.name(l)
        }
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "community".some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          boxTop(
            h1("Community blogs"),
            div(cls := "box__top__actions")(
              views.html.base.bits.mselect(
                "ublog-lang",
                lang.fold("All languages")(LangList.name),
                langSelections
                  .map { case (language, name) =>
                    a(
                      href := {
                        if language == "all" then routes.Ublog.communityAll()
                        else routes.Ublog.communityLang(language)
                      },
                      cls := (language == lang.fold("all")(_.language)).option("current")
                    )(name)
                  }
              ),
              a(
                cls      := "atom",
                st.title := "Atom RSS feed",
                href     := routes.Ublog.communityAtom(lang.fold("all")(_.language)),
                dataIcon := licon.RssFeed
              )
            )
          ),
          if posts.nbResults > 0 then
            div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, showAuthor = ShowAt.top) },
              pagerNext(
                posts,
                p =>
                  lang
                    .fold(routes.Ublog.communityAll(p))(l => routes.Ublog.communityLang(l.language, p))
                    .url
              )
            )
          else div(cls := "ublog-index__posts--empty")("Nothing to show.")
        )
      )
    }

  def topics(tops: List[UblogTopic.WithPosts])(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      title = "All blog topics"
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "topics".some),
        div(cls := "page-menu__content box")(
          boxTop(h1("All blog topics")),
          div(cls := "ublog-topics")(
            tops.map { case UblogTopic.WithPosts(topic, posts, nb) =>
              a(cls := "ublog-topics__topic", href := routes.Ublog.topic(topic.url))(
                h2(
                  topic.value,
                  span(cls := "ublog-topics__topic__nb")(trans.ublog.viewAllNbPosts(nb), " »")
                ),
                span(cls := "ublog-topics__topic__posts ublog-post-cards")(
                  posts map postView.miniCard
                )
              )
            }
          )
        )
      )
    }

  private def list(
      title: String,
      posts: Paginator[UblogPost.PreviewPost],
      menuItem: String,
      route: (Int, Option[Boolean]) => Call,
      onEmpty: => Frag,
      byDate: Option[Boolean] = None
  )(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = title
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, menuItem.some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          boxTop(
            h1(title),
            byDate.map: v =>
              span(
                "Sort by ",
                span(cls := "btn-rack")(
                  a(cls := s"btn-rack__btn${!v so " active"}", href := route(1, false.some))("rank"),
                  a(cls := s"btn-rack__btn${v so " active"}", href := route(1, true.some))("date")
                )
              )
          ),
          if posts.nbResults > 0 then
            div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, showAuthor = ShowAt.top) },
              pagerNext(posts, np => route(np, byDate).url)
            )
          else div(cls := "ublog-index__posts--empty")(onEmpty)
        )
      )
    }
