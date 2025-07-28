package lila.fide
package ui

import scalalib.paginator.Paginator

import lila.core.fide.FideTC
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class FideUi(helpers: Helpers)(menu: String => Context ?=> Frag):
  import helpers.{ *, given }
  import trans.{ site as trs, broadcast as trb }

  private val tcTrans: List[(FideTC, lila.core.i18n.I18nKey)] =
    List(
      FideTC.standard -> trs.classical,
      FideTC.rapid -> trs.rapid,
      FideTC.blitz -> trs.blitz
    )

  private def page(title: String, active: String)(modifiers: Modifier*)(using Context): Page =
    Page(title)
      .css("bits.fide")
      .js(infiniteScrollEsmInit ++ esmInitBit("fidePlayerFollow")):
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
        boxTop(h1(trb.fideFederations())),
        table(cls := "slist slist-pad")(
          thead:
            tr(
              th(trs.name()),
              th(trs.players()),
              th(trs.classical()),
              th(trs.rapid()),
              th(trs.blitz())
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
          div(h1(fed.name), p(trs.nbPlayers.plural(fed.nbPlayers, fed.nbPlayers.localize))),
          (fed.id.value == "KOS").option(p(cls := "fide-federation__kosovo")(kosovoText))
        ),
        div(cls := "fide-cards fide-federation__cards box__pad")(
          tcTrans.map: (tc, name) =>
            val stats = fed.stats(tc)
            card(
              name(),
              frag(
                p(trs.rank(), strong(stats.get.rank)),
                p(trb.top10Rating(), strong(stats.get.top10Rating)),
                p(trs.players(), strong(stats.get.nbPlayers.localize))
              )
            )
        ),
        player.playerList(
          players,
          np => routes.Fide.federation(fed.slug, np),
          withFlag = false
        )
      )

    private val kosovoText =
      """All reference to Kosovo, whether to the territory, institutions or population, in this text shall be understood in full compliance with United Nations Security Council Resolution 1244 and without prejudice to the status of Kosovo"""

    def flag(id: lila.core.fide.Federation.Id, title: Option[String]) = img(
      cls := "flag",
      st.title := title.getOrElse(id.value),
      src := assetUrl(s"images/fide-fed-webp/${id}.webp")
    )

    private def card(name: Frag, value: Frag) =
      div(cls := "fide-card fide-federation__card")(em(name), div(value))

  object player:

    def index(players: Paginator[FidePlayer], query: String)(using Context) =
      page("FIDE players", "players")(
        cls := "fide-players",
        boxTop(
          h1(trb.fidePlayers()),
          div(cls := "box__top__actions"):
            searchForm(query)
        ),
        playerList(players, np => routes.Fide.index(np, query.some.filter(_.nonEmpty)))
      )

    def notFound(id: chess.FideId)(using Context) =
      page("FIDE player not found", "players")(
        cls := "fide-players",
        boxTop(
          h1(trb.fidePlayerNotFound()),
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
          cls := "fide-players__search-form__input",
          name := "q",
          st.placeholder := "Search for players",
          st.value := q,
          autofocus := true,
          autocomplete := "off",
          spellcheck := "false"
        ),
        submitButton(cls := "button", dataIcon := Icon.Search)
      )

    def playerList(
        players: Paginator[FidePlayer],
        url: Int => Call,
        withFlag: Boolean = true
    )(using Context) =
      table(cls := "slist slist-pad")(
        thead:
          tr(
            th(trs.name()),
            withFlag.option(th(iconTag(Icon.FlagOutline))),
            th(trs.classical()),
            th(trs.rapid()),
            th(trs.blitz()),
            th(trb.ageThisYear())
          )
        ,
        tbody(cls := "infinite-scroll")(
          players.currentPageResults.map: player =>
            tr(cls := "paginated")(
              td(a(href := routes.Fide.show(player.id, player.slug))(titleTag(player.title), player.name)),
              withFlag.option(td:
                player.fed.map: fed =>
                  a(href := routes.Fide.federation(Federation.name(fed))):
                    federation.flag(fed, Federation.names.get(fed))),
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

    private def followButton(player: FidePlayer, isFollowing: Boolean)(using Context) =
      val id = "fide-player-follow"
      label(cls := "fide-player__follow")(
        form3.cmnToggle(
          fieldId = id,
          fieldName = id,
          checked = isFollowing,
          action = Some(routes.Fide.follow(player.id, isFollowing).url)
        ),
        trans.site.follow()
      )

    def show(player: FidePlayer, user: Option[User], tours: Option[Frag], isFollowing: Option[Boolean])(using
        Context
    ) =
      page(s"${player.name} - FIDE player ${player.id}", "players")(
        cls := "box-pad fide-player",
        div(cls := "fide-player__header")(
          h1(
            span(titleTag(player.title), player.name),
            user.map(userLink(_, withTitle = false)(cls := "fide-player__user"))
          ),
          isFollowing.map(followButton(player, _))
        ),
        div(cls := "fide-cards fide-player__cards")(
          player.fed.map: fed =>
            card(
              trb.federation(),
              a(cls := "fide-player__federation", href := routes.Fide.federation(Federation.idToSlug(fed)))(
                federation.flag(fed, none),
                Federation.name(fed)
              )
            ),
          card(
            trb.fideProfile(),
            a(href := s"https://ratings.fide.com/profile/${player.id}")(player.id)
          ),
          card(
            trb.ageThisYear(),
            player.age
          ),
          tcTrans.map: (tc, name) =>
            card(name(), player.ratingOf(tc).fold(trb.unrated())(_.toString))
        ),
        tours.map: tours =>
          div(cls := "fide-player__tours")(h2(trb.recentTournaments()), tours)
      )
