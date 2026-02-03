package lila.fide
package ui

import scalalib.paginator.Paginator
import chess.FideTC

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.I18nKey

final class FideUi(helpers: Helpers)(menu: String => Context ?=> Frag):
  import helpers.{ *, given }
  import trans.{ site as trs, broadcast as trb }

  private[ui] val tcTrans: List[(FideTC, I18nKey, Icon)] =
    List(
      (FideTC.standard, trs.classical, Icon.Turtle),
      (FideTC.rapid, trs.rapid, Icon.Rabbit),
      (FideTC.blitz, trs.blitz, Icon.Fire)
    )

  private[ui] def page(title: String, active: String, pageMods: Update[Page] = identity)(
      modifiers: Modifier*
  )(using
      Context
  ): Page =
    val editor = Granter.opt(_.FidePlayer)
    Page(title)
      .css("fide")
      .css(editor.option("fidePlayerForm"))
      .js(infiniteScrollEsmInit)
      .js(esmInit("fidePlayerFollow"))
      .js(editor.option(esmInit("fidePlayerForm")))
      .pipe(pageMods):
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

    def show(fed: Federation, playersList: Frag)(using Context) =
      page(s"${fed.name} - FIDE federation", "federations")(
        cls := "fide-federation",
        div(cls := "box__top fide-federation__head")(
          flag(fed.id, none),
          div(h1(fed.name), p(trs.nbPlayers.plural(fed.nbPlayers, fed.nbPlayers.localize))),
          (fed.id.value == "KOS").option(p(cls := "fide-federation__kosovo")(kosovoText))
        ),
        div(cls := "fide-cards fide-federation__cards box__pad")(
          tcTrans.map: (tc, name, icon) =>
            val stats = fed.stats(tc)
            card(
              em(dataIcon := icon, cls := "text")(name()),
              frag(
                p(trs.rank(), strong(stats.get.rank)),
                p(trb.top10Rating(), strong(stats.get.top10Rating)),
                p(trs.players(), strong(stats.get.nbPlayers.localize))
              )
            )
        ),
        playersList
      )

    private val kosovoText =
      """All reference to Kosovo, whether to the territory, institutions or population, in this text shall be understood in full compliance with United Nations Security Council Resolution 1244 and without prejudice to the status of Kosovo"""

    def flag(id: lila.core.fide.Federation.Id, title: Option[String]) = img(
      cls := "flag",
      st.title := title.getOrElse(id.value),
      src := fideFedSrc(id)
    )

    private def fideFedSrc(fideFed: lila.core.fide.Federation.Id): Url =
      staticAssetUrl(s"$fideFedVersion/fide/fed-webp/${fideFed}.webp")

    private def card(name: Frag, value: Frag) =
      div(cls := "fide-card fide-federation__card")(name, div(value))

  object player:

    private def card(name: Frag, value: Frag, icon: Option[Icon] = None) =
      div(cls := "fide-card fide-player__card")(
        em(dataIcon := icon, cls := List("text" -> icon.isDefined))(name),
        strong(value)
      )

    private def followButton(p: FidePlayer.WithFollow) =
      val id = s"fide-player-follow-${p.player.id}"
      label(cls := "fide-player__follow")(
        form3.cmnToggle(
          fieldId = id,
          fieldName = id,
          checked = p.follow,
          action = Some(routes.Fide.follow(p.player.id, p.follow).url),
          cssClass = "cmn-favourite"
        )
      )

    def show(player: FidePlayer, user: Option[User], tours: Option[Frag], isFollowing: Boolean)(using
        ctx: Context
    ) =
      page(s"${player.name} - FIDE player ${player.id}", "players")(
        cls := "box-pad fide-player",
        div(cls := "fide-player__header")(
          h1(
            span(titleTag(player.title), player.name),
            user.map(userLink(_, withTitle = false)(cls := "fide-player__user"))
          ),
          ctx.isAuth.option(followButton(FidePlayer.WithFollow(player, isFollowing))(trans.site.follow()))
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
            trb.age(),
            player.age
          ),
          tcTrans.map: (tc, name, icon) =>
            card(
              name(),
              player.ratingOf(tc).fold(trb.unrated())(_.toString),
              icon.some
            )
        ),
        tours.map: tours =>
          div(cls := "fide-player__tours")(h2(trb.recentTournaments()), tours)
      )
