package views.html
package forum

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

object bits {

  def searchForm(search: String = "")(implicit ctx: Context) =
    div(cls := "box__top__actions")(
      form(cls     := "search", action := routes.ForumPost.search())(
        input(name := "text", value    := search, placeholder := trans.search.search.txt())
      )
    )

  def pagination(route: Call, pager: Paginator[_], showPost: Boolean) =
    pager.hasToPaginate option {
      def url(page: Int) = s"$route?page=$page"
      st.nav(cls := "pagination")(
        pager.previousPage.fold(span(cls := "disabled", dataIcon := "I")) { prev => a(href := url(prev), dataIcon := "I") },
        pager.sliding(3, showPost = showPost).map {
          case None                              => raw(" &hellip; ")
          case Some(p) if p == pager.currentPage => span(cls := "current")(p)
          case Some(p)                           => a(href := url(p))(p)
        },
        pager.nextPage.fold(span(cls := "disabled", dataIcon := "H")) { next =>
          a(rel := "next", href := url(next), dataIcon := "H")
        }
      )
    }
  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
}
