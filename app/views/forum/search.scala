package views.html.forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes
import lila.team.Team

object search {

  def apply(text: String, pager: Paginator[lila.forum.PostView.WithReadPerm])(implicit
      ctx: Context
  ) = {
    val title = s"""${trans.search.search.txt()} "${text.trim}""""
    views.html.base.layout(
      title = title,
      moreJs = infiniteScrollTag,
      moreCss = cssTag("forum")
    )(
      main(cls := "box search")(
        div(cls := "box__top")(
          h1(
            a(href := routes.ForumCateg.index, dataIcon := "", cls := "text"),
            title
          ),
          bits.searchForm(text)
        ),
        strong(cls := "nb-results box__pad")(pager.nbResults, " posts found"),
        table(cls := "slist slist-pad search__results")(
          if (pager.nbResults > 0)
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map { viewWithRead =>
                val view = viewWithRead.view
                val info =
                  td(cls := "info")(
                    momentFromNow(view.post.createdAt),
                    br,
                    bits.authorLink(view.post)
                  )
                tr(cls := "paginated")(
                  if (viewWithRead.canRead)
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
              },
              pagerNextTable(pager, n => routes.ForumPost.search(text, n).url)
            )
          else tbody(tr(td("No forum post found")))
        )
      )
    )
  }
}
