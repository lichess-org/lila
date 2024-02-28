package views.html.fide

import play.api.mvc.Call
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FidePlayer, Federation }
import lila.common.paginator.Paginator
import lila.relay.RelayTour

object player:

  def index(players: Paginator[FidePlayer], query: String)(using PageContext) =
    bits.layout("FIDE players", "players")(
      cls := "fide-players",
      boxTop(
        h1("FIDE players"),
        div(cls := "box__top__actions"):
          searchForm(query)
      ),
      playerList(players, np => routes.Fide.index(np, query.some.filter(_.nonEmpty)))
    )

  def searchForm(q: String) =
    st.form(cls := "fide-players__search-form", action := routes.Fide.index(1), method := "get")(
      input(
        cls            := "fide-players__search-form__input",
        name           := "q",
        st.placeholder := "Search for players",
        st.value       := q,
        autofocus      := true,
        autocomplete   := "off",
        spellcheck     := "false"
      ),
      submitButton(cls := "button", dataIcon := licon.Search)
    )

  def playerList(
      players: Paginator[FidePlayer],
      url: Int => Call,
      withFlag: Boolean = true,
      title: String = "Name"
  )(using Context) =
    table(cls := "slist slist-pad")(
      thead:
        tr(
          th(title),
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
                a(href := routes.Fide.federation(Federation.name(fed)))(
                  federation.flag(fed, Federation.name(fed))
                )
            ,
            td(player.standard),
            td(player.rapid),
            td(player.blitz),
            td(player.age)
          ),
        pagerNextTable(players, np => url(np).url)
      )
    )

  private def card(name: Frag, value: Frag) =
    div(cls := "fide-card fide-player__card")(em(name), strong(value))

  def show(player: FidePlayer, tours: Paginator[RelayTour.WithLastRound])(using PageContext) =
    bits.layout(s"${player.name} - FIDE player ${player.id}", "players")(
      cls := "box-pad fide-player",
      h1(titleTag(player.title), player.name),
      div(cls := "fide-cards fide-player__cards")(
        player.fed.map: fed =>
          card(
            "Federation",
            a(href := routes.Fide.federation(Federation.idToSlug(fed)))(
              federation.flag(fed, fed),
              Federation.name(fed)
            )
          ),
        card(
          "FIDE profile",
          a(href := s"https://ratings.fide.com/profile/${player.id}")(player.id)
        ),
        card(
          "Age",
          player.age
        ),
        bits.tcTrans.map: (tc, name) =>
          card(name(), player.ratingOf(tc).fold("-")(_.toString))
      ),
      tours.nbResults > 0 option div(cls := "fide-player__tours")(
        h2("Recent tournaments"),
        views.html.relay.tour.renderPager(views.html.relay.tour.asRelayPager(tours)): page =>
          routes.Fide.show(player.id, player.slug, page)
      )
    )
