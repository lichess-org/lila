package views.html.forum

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

object search:

  def apply(text: String, pager: Paginator[lila.forum.PostView.WithReadPerm])(using PageContext) =
    val title = s"""${trans.search.search.txt()} "${text.trim}""""
    views.html.base.layout(
      title = title,
      modules = infiniteScrollTag,
      moreCss = cssTag("forum")
    )(
      main(cls := "box search")(
        boxTop(
          h1(
            a(href := routes.ForumCateg.index, dataIcon := licon.LessThan, cls := "text"),
            title
          ),
          bits.searchForm(text)
        ),
        strong(cls := "nb-results box__pad")(trans.site.nbForumPosts.pluralSame(pager.nbResults)),
        table(cls := "slist slist-pad search__results")(
          (pager.nbResults > 0).option(
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map: viewWithRead =>
                val view = viewWithRead.view
                val info =
                  td(cls := "info")(
                    momentFromNow(view.post.createdAt),
                    br,
                    bits.authorLink(view.post)
                  )
                tr(cls := "paginated")(
                  if viewWithRead.canRead then
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
                  else td(colspan := "2")("[You can't access this team forum post]")
                )
              ,
              pagerNextTable(pager, n => routes.ForumPost.search(text, n).url)
            )
          )
        )
      )
    )
