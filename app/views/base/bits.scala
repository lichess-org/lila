package views.html
package base

import chess.format.Fen
import controllers.routes
import play.api.i18n.Lang
import play.api.mvc.Call

import lila.app.templating.Environment.*
import lila.web.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

object bits:

  def mselect(id: String, current: Frag, items: Seq[Tag]) =
    div(cls := "mselect")(
      input(
        tpe          := "checkbox",
        cls          := "mselect__toggle fullscreen-toggle",
        st.id        := s"mselect-$id",
        autocomplete := "off"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items.map(_(cls := "mselect__item")))
    )

  lazy val stage = a(
    href  := "https://lichess.org",
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
  ):
    "This is an empty Lichess preview website, go to lichess.org instead"

  val connectLinks =
    div(cls := "connect-links")(
      a(
        href := routes.Main.externalLink("mastodon", "https://mastodon.online/@lichess"),
        targetBlank,
        noFollow,
        relMe
      )("Mastodon"),
      a(href := routes.Main.externalLink("twitter", "https://twitter.com/lichess"), targetBlank, noFollow)(
        "Twitter"
      ),
      a(href := routes.Main.externalLink("discord", "https://discord.gg/lichess"), targetBlank, noFollow)(
        "Discord"
      ),
      a(
        href := routes.Main.externalLink("youtube", "https://youtube.com/c/LichessDotOrg"),
        targetBlank,
        noFollow
      )("YouTube"),
      a(
        href := routes.Main.externalLink("twitch", "https://www.twitch.tv/lichessdotorg"),
        targetBlank,
        noFollow
      )("Twitch"),
      a(
        href := routes.Main.externalLink("instagram", "https://instagram.com/lichessdotorg"),
        targetBlank,
        noFollow
      )("Instagram")
    )

  def fenAnalysisLink(fen: Fen.Full)(using Translate) =
    a(href := routes.UserAnalysis.parseArg(underscoreFen(fen)))(trans.site.analysis())

  def paginationByQuery(route: Call, pager: Paginator[?], showPost: Boolean): Option[Frag] =
    pagination(page => s"$route?page=$page", pager, showPost)

  def pagination(url: Int => String, pager: Paginator[?], showPost: Boolean): Option[Frag] =
    pager.hasToPaginate.option(pagination(url, pager.currentPage, pager.nbPages, showPost))

  def pagination(url: Int => String, page: Int, nbPages: Int, showPost: Boolean): Tag =
    st.nav(cls := "pagination")(
      if page > 1
      then a(href   := url(page - 1), dataIcon := Icon.LessThan)
      else span(cls := "disabled", dataIcon    := Icon.LessThan),
      sliding(page, nbPages, 3, showPost = showPost).map:
        case None                 => raw(" &hellip; ")
        case Some(p) if p == page => span(cls := "current")(p)
        case Some(p)              => a(href := url(p))(p)
      ,
      if page < nbPages
      then a(rel    := "next", dataIcon     := Icon.GreaterThan, href := url(page + 1))
      else span(cls := "disabled", dataIcon := Icon.GreaterThan)
    )

  private def sliding(page: Int, nbPages: Int, length: Int, showPost: Boolean): List[Option[Int]] =
    val fromPage = 1.max(page - length)
    val toPage   = nbPages.min(page + length)
    val pre = fromPage match
      case 1 => Nil
      case 2 => List(1.some)
      case _ => List(1.some, none)
    val post = toPage match
      case x if x == nbPages     => Nil
      case x if x == nbPages - 1 => List(nbPages.some)
      case _ if showPost         => List(none, nbPages.some)
      case _                     => List(none)
    pre ::: (fromPage to toPage).view.map(some).toList ::: post

  def ariaTabList(prefix: String, selected: String)(tabs: (String, String, Frag)*) = frag(
    div(cls := "tab-list", role := "tablist")(
      tabs.map: (id, name, _) =>
        button(
          st.id            := s"$prefix-tab-$id",
          aria("controls") := s"$prefix-panel-$id",
          role             := "tab",
          cls              := "tab-list__tab",
          aria("selected") := (selected == id).option("true"),
          tabindex         := 0
        )(name)
    ),
    div(cls := "panel-list")(
      tabs.map: (id, _, content) =>
        div(
          st.id              := s"$prefix-panel-$id",
          aria("labelledby") := s"$prefix-tab-$id",
          role               := "tabpanel",
          cls                := List("panel-list__panel" -> true, "none" -> (selected != id)),
          tabindex           := 0
        )(content)
    )
  )
