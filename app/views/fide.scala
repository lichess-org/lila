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
          tr(cls := "paginated")(
            td(a(href := routes.Fide.show(player.id, player.slug))(player.name)),
            td:
              player.fed.map: fed =>
                fedFlag(fed, Federation.name(fed))
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

  def federations(feds: Paginator[Federation])(using PageContext) =
    layout("FIDE federations"):
      main(cls := "page-small box page fide-federations")(
        boxTop(
          h1("FIDE federations"),
          div(cls := "box__top__actions"):
            input(cls := "fide__search", placeholder := trans.search.search.txt(), autofocus)
        ),
        federationList(feds)
      )

  private def federationList(feds: Paginator[Federation])(using Context) =
    def ratingCell(stats: Federation.Stats) =
      td(if stats.top10Rating > 0 then stats.top10Rating else "-")
    table(cls := "slist slist-pad")(
      thead:
        tr(
          th("Name"),
          th("Players"),
          th("Classic"),
          th("Rapid"),
          th("Blitz")
        )
      ,
      tbody(cls := "infinite-scroll")(
        feds.currentPageResults.map: fed =>
          tr(cls := "paginated")(
            td(a(href := routes.Fide.federation(fed.slug))(fedFlag(fed.id, fed.id), fed.name)),
            td(fed.nbPlayers.localize),
            ratingCell(fed.standard),
            ratingCell(fed.rapid),
            ratingCell(fed.blitz)
          ),
        pagerNextTable(feds, np => routes.Fide.federations(np).url)
      )
    )

  private def fedFlag(id: Federation.Id, title: String) = img(
    cls      := "flag",
    st.title := title,
    src      := assetUrl(s"images/fide-fed/${id}.svg")
  )

  def federation(fed: Federation)(using PageContext) =
    layout(s"$name - FIDE federation"):
      main(cls := "page-small box box-pad fide-player")(
        h1(a(href := routes.Fide.federations(1))("Federations"), " • ", fed.name)
      )
