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
