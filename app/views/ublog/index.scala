package views.html.ublog

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.{ UblogPost, UblogTopic }
import lila.user.User
import play.api.i18n.Lang
import lila.i18n.LangList

object index {

  import views.html.ublog.{ post => postView }

  def drafts(user: User, posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = trans.ublog.drafts.txt()
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "mine".some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(
            h1(trans.ublog.drafts()),
            div(cls := "box__top__actions")(
              a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
              postView.newPostLink
            )
          ),
          if (posts.nbResults > 0)
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

  def friends(posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) = list(
    title = "Friends blogs",
    posts = posts,
    menuItem = "friends",
    route = routes.Ublog.friends _,
    onEmpty = "Nothing to show. Follow some authors!"
  )

  def liked(posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) = list(
    title = "Liked blog posts",
    posts = posts,
    menuItem = "liked",
    route = routes.Ublog.liked _,
    onEmpty = "Nothing to show. Like some posts!"
  )

  def topic(top: UblogTopic, posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) = list(
    title = s"Blog posts about $top",
    posts = posts,
    menuItem = "topics",
    route = p => routes.Ublog.topic(top.value, p),
    onEmpty = "Nothing to show."
  )

  def community(lang: Option[Lang], posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = "Community blogs",
      atomLinkTag = link(
        href     := routes.Ublog.communityAtom(lang.fold("all")(_.code)),
        st.title := "Lichess community blogs"
      ).some
    ) {
      val langSelections = ("all", "All languages") :: lila.i18n.I18nLangPicker
        .sortFor(LangList.popularNoRegion, ctx.req)
        .map { l =>
          l.code -> LangList.name(l)
        }
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "community".some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(
            h1("Community blogs"),
            div(cls := "box__top__actions")(
              views.html.base.bits.mselect(
                "ublog-lang",
                lang.fold("All languages")(LangList.name),
                langSelections
                  .map { case (code, name) =>
                    a(
                      href := routes.Ublog.community(code),
                      cls  := (code == lang.fold("all")(_.code)).option("current")
                    )(name)
                  }
              ),
              a(
                cls      := "atom",
                st.title := "Atom RSS feed",
                href     := routes.Ublog.communityAtom(lang.fold("all")(_.code)),
                dataIcon := ""
              )
            )
          ),
          if (posts.nbResults > 0)
            div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, showAuthor = true) },
              pagerNext(posts, p => routes.Ublog.community(lang.fold("all")(_.code), p).url)
            )
          else
            div(cls := "ublog-index__posts--empty")("Nothing to show.")
        )
      )
    }

  def topics(tops: List[UblogTopic.WithPosts])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      title = "All blog topics"
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "topics".some),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(h1("All blog topics")),
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
      route: Int => Call,
      onEmpty: => Frag
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = title
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, menuItem.some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(h1(title)),
          if (posts.nbResults > 0)
            div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, showAuthor = true) },
              pagerNext(posts, np => route(np).url)
            )
          else
            div(cls := "ublog-index__posts--empty")(onEmpty)
        )
      )
    }
}
