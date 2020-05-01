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
      side = side(
        none,
        champions,
        "/games",
        povOption = none,
        customTitle = if (povs.isEmpty) " - " else trans.nbGames.pluralSameTxt(povs.length)
      ).map(_.toHtml),
      moreCss = cssTags("tv.css", "form3.css"),
      moreJs = frag(
        jsTag("custom-games.js"),
        embedJs(s"""lidraughts=lidraughts||{};lidraughts.collectionI18n=${jsI18n()}""")
      )
    ) {
        div(cls := "games_playing")(
          div(cls := "game_list playing")(
            povs.map { p =>
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
