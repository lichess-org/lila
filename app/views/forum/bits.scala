package views.html
package forum

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object bits {

  def layout(
    title: String,
    moreJs: Frag = emptyFrag,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag("forum"),
      moreJs = moreJs,
      openGraph = openGraph
    )(body)

  def searchForm(search: String = "")(implicit ctx: Context) =
    form(cls := "search", action := routes.ForumPost.search())(
      input(name := "text", value := search, placeholder := trans.search.txt())
    )

  def pagination(route: Call, pager: Paginator[_], showPost: Boolean) = pager.hasToPaginate option {
    def url(page: Int) = s"$route?page=$page"
    st.nav(cls := "pagination")(
      if (pager.hasPreviousPage) a(href := url(pager.previousPage.get), dataIcon := "I")
      else span(cls := "disabled", dataIcon := "I"),
      pager.sliding(3, showPost = showPost).map {
        case None => frag(" ... ")
        case Some(p) if p == pager.currentPage => span(cls := "current")(p)
        case Some(p) => a(href := url(p))(p)
      },
      if (pager.hasNextPage) a(rel := "next", href := url(pager.nextPage.get), dataIcon := "H")
      else span(cls := "disabled", dataIcon := "H")
    )
  }
  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
}
