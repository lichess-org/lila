package views.html.fide

import play.api.mvc.Call
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FidePlayer, Federation }
import lila.common.paginator.Paginator

object player:

  def index(players: Paginator[FidePlayer])(using PageContext) =
    bits.layout("FIDE players"):
      main(cls := "page-small box page fide-players")(
        boxTop(
          h1("FIDE players"),
          div(cls := "box__top__actions"):
            input(cls := "fide__search", placeholder := trans.search.search.txt(), autofocus)
        ),
        playerList(players, np => routes.Fide.index(np))
      )

  def playerList(players: Paginator[FidePlayer], url: Int => Call, withFlag: Boolean = true)(using Context) =
    table(cls := "slist slist-pad")(
      thead:
        tr(
          th("Name"),
          withFlag option th(iconTag(licon.FlagOutline)),
          th("Classic"),
          th("Rapid"),
          th("Blitz"),
          th("Age")
        )
      ,
      tbody(cls := "infinite-scroll")(
        players.currentPageResults.map: player =>
          tr(cls := "paginated")(
            td(a(href := routes.Fide.show(player.id, player.slug))(player.name)),
            withFlag option td:
              player.fed.map: fed =>
                federation.flag(fed, Federation.name(fed))
            ,
            td(player.standard),
            td(player.rapid),
            td(player.blitz),
            td(player.age)
          ),
        pagerNextTable(players, np => url(np).url)
      )
    )

  def show(player: FidePlayer)(using PageContext) =
    bits.layout(s"${player.name} - FIDE player ${player.id}"):
      main(cls := "page-small box box-pad fide-player")(
        h1(a(href := routes.Fide.index())("FIDE players"), " • ", player.name)
      )
