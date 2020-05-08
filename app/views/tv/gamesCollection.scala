package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object gamesCollection {

  def apply(povs: List[lidraughts.game.Pov], champions: lidraughts.tv.Tv.Champions)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.customGamesCollection.txt(),
      moreCss = cssTag("tv.games"),
      moreJs = frag(
        jsTag("custom-games.js"),
        embedJsUnsafe(s"""lidraughts=lidraughts||{};lidraughts.collectionI18n=${jsI18n()}""")
      )
    ) {
        main(cls := "page-menu tv-games")(
          st.aside(cls := "page-menu__menu")(
            side.channels(
              channel = none,
              champions = champions,
              baseUrl = "/games",
              customTitle = povs.nonEmpty option trans.nbGames.pluralSameTxt(povs.length)
            )
          ),
          div(cls := "page-menu__content now-playing editable")(
            povs map { p =>
              div(views.html.game.bits.mini(p))
            }
          )
        )
      }

  private def jsI18n()(implicit ctx: Context) = safeJsonValue(i18nJsObject(translations))
  private val translations = List(
    trans.nbGames,
    trans.flipBoard,
    trans.removeGame
  )
}
