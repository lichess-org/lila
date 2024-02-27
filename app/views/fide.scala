package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.player.FidePlayer
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
      main(cls := "page-small box box-pad page")(
        div(cls := "fide box box-pad")(
          boxTop(
            h1("FIDE players"),
            div(cls := "box__top__actions"):
              input(cls := "fide__search", placeholder := trans.search.search.txt(), autofocus)
          ),
          playerList(players)
        )
      )

  def playerList(players: Paginator[FidePlayer])(using Context) =
    div(cls := "fide__players infinite-scroll")(
      players.currentPageResults
        .map: player =>
          div(cls := "fide__player paginated", id := player.id)(
            player.name
          ),
      pagerNext(players, np => routes.Fide.index(np).url)
    )
