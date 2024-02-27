package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FidePlayer, Federation }
import lila.common.paginator.Paginator

object fide:

  private def layout(title: String)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("fide"),
      title = title,
      moreJs = frag(infiniteScrollTag)
    )

  def index(players: Paginator[FidePlayer])(using PageContext) =
    layout("FIDE players"):
      main(cls := "page-small box page fide-players")(
        boxTop(
          h1("FIDE players"),
          div(cls := "box__top__actions"):
            input(cls := "fide__search", placeholder := trans.search.search.txt(), autofocus)
        ),
        playerList(players)
      )

  private def playerList(players: Paginator[FidePlayer])(using Context) =
    table(cls := "slist slist-pad")(
      thead:
        tr(
          th("Name"),
          th(iconTag(licon.FlagOutline)),
          th("Classic"),
          th("Rapid"),
          th("Blitz"),
          th("Age")
        )
      ,
      tbody(cls := "infinite-scroll")(
        players.currentPageResults.map: player =>
          tr(cls := "paginated", id := player.id)(
            td(a(href := routes.Fide.show(player.id, player.slug))(player.name)),
            td:
              player.fed.map: fed =>
                img(
                  cls   := "flag",
                  title := Federation.name(fed),
                  src   := assetUrl(s"images/fide-fed/${fed}.svg")
                )
            ,
            td(player.standard),
            td(player.rapid),
            td(player.blitz),
            td(player.age)
          ),
        pagerNextTable(players, np => routes.Fide.index(np).url)
      )
    )

  def show(player: FidePlayer)(using PageContext) =
    layout(s"${player.name} - FIDE player ${player.id}"):
      main(cls := "page-small box box-pad fide-player")(
        h1(a(href := routes.Fide.index())("FIDE players"), " • ", player.name)
      )

  def federation(code: String, name: String)(using PageContext) =
    layout(s"$name - FIDE federation"):
      main(cls := "page-small box box-pad fide-player")(
        h1(a(href := routes.Fide.federations)("Federations"), " • ", name)
      )
