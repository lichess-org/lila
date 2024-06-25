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
      .css("bits.fide")
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
        boxTop(h1(trans.site.fideFederations())),
        table(cls := "slist slist-pad")(
          thead:
            tr(
              th(trans.site.name()),
              th(trans.site.players()),
              th(trans.site.classical()),
              th(trans.site.rapid()),
              th(trans.site.blitz())
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
                p(trans.site.rank(), strong(stats.get.rank)),
                p(trans.site.top10Rating(), strong(stats.get.top10Rating)),
                p(trans.site.players(), strong(stats.get.nbPlayers.localize))
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
          h1(trans.site.fidePlayers()),
          div(cls := "box__top__actions"):
            searchForm(query)
        ),
        playerList(players, np => routes.Fide.index(np, query.some.filter(_.nonEmpty)))
      )

    def notFound(id: chess.FideId)(using Context) =
      page("FIDE player not found", "players")(
        cls := "fide-players",
        boxTop(
          h1(trans.site.fidePlayerNotFound()),
          div(cls := "box__top__actions"):
            searchForm("")
        ),
        div(cls := "box__pad")(
          p(
            "We could not find anyone with the FIDE ID \"",
            strong(id),
            "\", please make sure the number is correct."
          ),
          p(
            "If the player appears on the ",
            a(href := "https://ratings.fide.com/", targetBlank)("official FIDE website"),
            ", then the player was not included in the latest rating export from FIDE.",
            br,
            "FIDE exports are provided once a month and includes players who have at least one official rating."
          )
        )
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
            th(trans.site.name),
            withFlag.option(th(iconTag(Icon.FlagOutline))),
            th(trans.site.classical()),
            th(trans.site.rapid()),
            th(trans.site.blitz()),
            th(trans.site.ageThisYear())
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
              trans.site.federation(),
              if fed == Federation.idNone then "None"
              else
                a(cls := "fide-player__federation", href := routes.Fide.federation(Federation.idToSlug(fed)))(
                  federation.flag(fed, none),
                  Federation.name(fed)
                )
            ),
          card(
            trans.site.fideProfile(),
            a(href := s"https://ratings.fide.com/profile/${player.id}")(player.id)
          ),
          card(
            trans.site.ageThisYear(),
            player.age
          ),
          tcTrans.map: (tc, name) =>
            card(name(), player.ratingOf(tc).fold(trans.site.unrated())(_.toString)),
        ),
        tours.map: tours =>
          div(cls := "fide-player__tours")(h2(trans.site.recentTournaments()), tours)
      )
