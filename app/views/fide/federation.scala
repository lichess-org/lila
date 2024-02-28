package views.html.fide

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FidePlayer, Federation }
import lila.common.paginator.Paginator

object federation:

  def index(feds: Paginator[Federation])(using PageContext) =
    bits.layout("FIDE federations"):
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
            td(a(href := routes.Fide.federation(fed.slug))(flag(fed.id, fed.id), fed.name)),
            td(fed.nbPlayers.localize),
            ratingCell(fed.standard),
            ratingCell(fed.rapid),
            ratingCell(fed.blitz)
          ),
        pagerNextTable(feds, np => routes.Fide.federations(np).url)
      )
    )

  def flag(id: Federation.Id, title: String) = img(
    cls      := "flag",
    st.title := title,
    src      := assetUrl(s"images/fide-fed/${id}.svg")
  )

  def show(fed: Federation, players: Paginator[FidePlayer])(using PageContext) =
    bits.layout(s"${fed.name} - FIDE federation"):
      main(cls := "page-small box fide-federation")(
        div(cls := "box__top")(
          h1(a(href := routes.Fide.federations(1))("Federations"), " • ", flag(fed.id, fed.id), fed.name)
        ),
        player.playerList(players, np => routes.Fide.federation(fed.slug, np), withFlag = false)
      )
