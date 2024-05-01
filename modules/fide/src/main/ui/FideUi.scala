package lila.fide
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.fide.FideTC

final class FideUi(helpers: Helpers)(menu: String => Context ?=> Frag):
  import helpers.{ *, given }

  private val tcTrans: List[(FideTC, lila.core.i18n.I18nKey)] =
    List(
      FideTC.standard -> trans.site.classical,
      FideTC.rapid    -> trans.site.rapid,
      FideTC.blitz    -> trans.site.blitz
    )

  private def page(title: String, active: String)(modifiers: Modifier*)(using Context): Page =
    Page(title)
      .cssTag("fide")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu(active),
          div(cls := "page-menu__content box")(modifiers)
        )

  object federation:

    def index(feds: Paginator[Federation])(using Context) =
      def ratingCell(stats: lila.core.fide.Federation.Stats) =
        td(if stats.top10Rating > 0 then stats.top10Rating else "-")
      page("FIDE federations", "federations")(
        cls := "fide-federations",
        boxTop(h1("FIDE federations")),
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
      )

    def show(fed: Federation, players: Paginator[FidePlayer])(using Context) =
      page(s"${fed.name} - FIDE federation", "federations")(
        cls := "fide-federation",
        div(cls := "box__top fide-federation__head")(
          flag(fed.id, none),
          div(h1(fed.name), p(trans.site.nbPlayers.plural(fed.nbPlayers, fed.nbPlayers.localize))),
          (fed.id.value == "KOS").option(p(cls := "fide-federation__kosovo")(kosovoText))
        ),
        div(cls := "fide-cards fide-federation__cards box__pad")(
          tcTrans.map: (tc, name) =>
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

    def flag(id: lila.core.fide.Federation.Id, title: Option[String]) = img(
      cls      := "flag",
      st.title := title.getOrElse(id.value),
      src      := assetUrl(s"images/fide-fed/${id}.svg")
    )

    private def card(name: Frag, value: Frag) =
      div(cls := "fide-card fide-federation__card")(em(name), div(value))

  object player:

    def index(players: Paginator[FidePlayer], query: String)(using Context) =
      page("FIDE players", "players")(
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
        submitButton(cls := "button", dataIcon := Icon.Search)
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
            withFlag.option(th(iconTag(Icon.FlagOutline))),
            th("Classic"),
            th("Rapid"),
            th("Blitz"),
            th("Age this year")
          )
        ,
        tbody(cls := "infinite-scroll")(
          players.currentPageResults.map: player =>
            tr(cls := "paginated")(
              td(a(href := routes.Fide.show(player.id, player.slug))(titleTag(player.title), player.name)),
              withFlag.option(td:
                player.fed.map: fed =>
                  a(href := routes.Fide.federation(Federation.name(fed))):
                    federation.flag(fed, Federation.names.get(fed))
              ),
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

    def show(player: FidePlayer, tours: Option[Frag])(using Context) =
      page(s"${player.name} - FIDE player ${player.id}", "players")(
        cls := "box-pad fide-player",
        h1(titleTag(player.title), player.name),
        div(cls := "fide-cards fide-player__cards")(
          player.fed.map: fed =>
            card(
              "Federation",
              a(cls := "fide-player__federation", href := routes.Fide.federation(Federation.idToSlug(fed)))(
                federation.flag(fed, none),
                Federation.name(fed)
              )
            ),
          card(
            "FIDE profile",
            a(href := s"https://ratings.fide.com/profile/${player.id}")(player.id)
          ),
          card(
            "Age this year",
            player.age
          ),
          tcTrans.map: (tc, name) =>
            card(name(), player.ratingOf(tc).fold("-")(_.toString))
        ),
        tours.map: tours =>
          div(cls := "fide-player__tours")(h2("Recent tournaments"), tours)
      )
