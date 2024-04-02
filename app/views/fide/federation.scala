package views.html.fide

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator
import lila.fide.{ Federation, FidePlayer }

object federation:

  def index(feds: Paginator[Federation])(using PageContext) =
    bits.layout("FIDE federations", "federations")(
      cls := "fide-federations",
      boxTop(
        h1("FIDE federations")
      ),
      federationList(feds)
    )

  private def federationList(feds: Paginator[Federation])(using Context) =
    def ratingCell(stats: lila.core.fide.Federation.Stats) =
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
            td(a(href := routes.Fide.federation(fed.slug))(flag(fed.id, none), fed.name)),
            td(fed.nbPlayers.localize),
            ratingCell(fed.standard),
            ratingCell(fed.rapid),
            ratingCell(fed.blitz)
          ),
        pagerNextTable(feds, np => routes.Fide.federations(np).url)
      )
    )

  def flag(id: lila.core.fide.Federation.Id, title: Option[String]) = img(
    cls      := "flag",
    st.title := title.getOrElse(id.value),
    src      := assetUrl(s"images/fide-fed/${id}.svg")
  )

  private def card(name: Frag, value: Frag) =
    div(cls := "fide-card fide-federation__card")(em(name), div(value))

  def show(fed: Federation, players: Paginator[FidePlayer])(using PageContext) =
    bits.layout(s"${fed.name} - FIDE federation", "federations")(
      cls := "fide-federation",
      div(cls := "box__top fide-federation__head")(
        flag(fed.id, none),
        div(h1(fed.name), p(trans.site.nbPlayers.plural(fed.nbPlayers, fed.nbPlayers.localize))),
        (fed.id.value == "KOS").option(p(cls := "fide-federation__kosovo")(kosovoText))
      ),
      div(cls := "fide-cards fide-federation__cards box__pad")(
        bits.tcTrans.map: (tc, name) =>
          val stats = fed.stats(tc)
          card(
            name(),
            frag(
              p("Rank", strong(stats.get.rank)),
              p("Top 10 rating", strong(stats.get.top10Rating)),
              p("Players", strong(stats.get.nbPlayers.localize))
            )
          )
      ),
      player.playerList(
        players,
        np => routes.Fide.federation(fed.slug, np),
        withFlag = false,
        title = "Players"
      )
    )

  private val kosovoText =
    """All reference to Kosovo, whether to the territory, institutions or population, in this text shall be understood in full compliance with United Nations Security Council Resolution 1244 and without prejudice to the status of Kosovo"""
