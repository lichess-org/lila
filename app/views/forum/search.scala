package views.html.forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.Team

import controllers.routes

object search {

  def apply(text: String, pager: Paginator[lila.forum.PostView], myTeamIds: Set[Team.ID])(implicit
      ctx: Context
  ) = {
    val title = s"""${trans.search.search.txt()} "${text.trim}""""
    views.html.base.layout(
      title = title,
      moreJs = infiniteScrollTag,
      moreCss = cssTag("forum")
    )(
      main(cls := "box box search")(
        div(cls := "box__top")(
          h1(
            a(href := routes.ForumCateg.index, dataIcon := "I", cls := "text"),
            title
          ),
          bits.searchForm(text)
        ),
        strong(cls := "nb-results box__pad")(trans.nbForumPosts.pluralSame(pager.nbResults)),
        table(cls := "slist slist-pad search__results")(
          (pager.nbResults > 0) option
            tbody(cls := "infinitescroll")(
              pagerNextTable(pager, n => routes.ForumPost.search(text, n).url) | tr,
              pager.currentPageResults.map { view =>
                val info =
                  td(cls := "info")(
                    momentFromNow(view.post.createdAt),
                    br,
                    authorLink(view.post, modIcon = ~view.post.modIcon)
                  )
                tr(cls := "paginated")(
                  if (view.categ.team.forall(myTeamIds.contains))
                    frag(
                      td(
                        a(cls := "post", href := routes.ForumPost.redirect(view.post.id))(
                          view.categ.name,
                          " - ",
                          view.topic.name,
                          "#",
                          view.post.number
                        ),
                        p(shorten(view.post.text, 200))
                      ),
                      info
                    )
                  else
                    frag(
                      td("[You can't access this team forum post]"),
                      info
                    )
                )
              }
            )
        )
      )
    )
  }
}
