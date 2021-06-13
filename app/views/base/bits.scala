package views.html
package base

import chess.format.FEN
import controllers.routes
import play.api.i18n.Lang
import play.api.mvc.Call

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

object bits {

  def mselect(id: String, current: Frag, items: List[Frag]) =
    div(cls := "mselect")(
      input(
        tpe := "checkbox",
        cls := "mselect__toggle fullscreen-toggle",
        st.id := s"mselect-$id",
        autocomplete := "off"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items)
    )

  lazy val stage = a(
    href := "https://lichess.org",
    style := """
background: #7f1010;
color: #fff;
position: fixed;
bottom: 0;
left: 0;
padding: .5em 1em;
border-top-right-radius: 3px;
z-index: 99;
"""
  )(
    "This is an empty Lichess preview website, go to lichess.org instead"
  )

  val connectLinks =
    div(cls := "connect-links")(
      a(href := "https://twitter.com/lichess", targetBlank, noFollow)("Twitter"),
      a(href := "https://discord.gg/lichess", targetBlank, noFollow)("Discord"),
      a(href := "https://www.youtube.com/channel/UCr6RfQga70yMM9-nuzAYTsA", targetBlank, noFollow)("YouTube"),
      a(href := "https://www.twitch.tv/lichessdotorg", targetBlank, noFollow)("Twitch")
    )

  def fenAnalysisLink(fen: FEN)(implicit lang: Lang) =
    a(href := routes.UserAnalysis.parseArg(fen.value.replace(" ", "_")))(trans.analysis())

  def paginationByQuery(route: Call, pager: Paginator[_], showPost: Boolean): Option[Frag] =
    pagination(page => s"$route?page=$page", pager, showPost)

  def pagination(url: Int => String, pager: Paginator[_], showPost: Boolean): Option[Frag] =
    pager.hasToPaginate option pagination(url, pager.currentPage, pager.nbPages, showPost)

  def pagination(url: Int => String, page: Int, nbPages: Int, showPost: Boolean): Tag =
    st.nav(cls := "pagination")(
      if (page > 1) a(href := url(page - 1), dataIcon := "")
      else span(cls := "disabled", dataIcon := ""),
      sliding(page, nbPages, 3, showPost = showPost).map {
        case None                 => raw(" &hellip; ")
        case Some(p) if p == page => span(cls := "current")(p)
        case Some(p)              => a(href := url(p))(p)
      },
      if (page < nbPages) a(rel := "next", href := url(page + 1), dataIcon := "")
      else span(cls := "disabled", dataIcon := "")
    )

  private def sliding(pager: Paginator[_], length: Int, showPost: Boolean): List[Option[Int]] =
    sliding(pager.currentPage, pager.nbPages, length, showPost)

  private def sliding(page: Int, nbPages: Int, length: Int, showPost: Boolean): List[Option[Int]] = {
    val fromPage = 1 max (page - length)
    val toPage   = nbPages.min(page + length)
    val pre = fromPage match {
      case 1 => Nil
      case 2 => List(1.some)
      case _ => List(1.some, none)
    }
    val post = toPage match {
      case x if x == nbPages     => Nil
      case x if x == nbPages - 1 => List(nbPages.some)
      case _ if showPost         => List(none, nbPages.some)
      case _                     => List(none)
    }
    pre ::: (fromPage to toPage).view.map(Some.apply).toList ::: post
  }
}
