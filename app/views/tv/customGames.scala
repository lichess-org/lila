package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object customGames {

  def apply(povs: List[lidraughts.game.Pov])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Custom games list",
      side = side(
        none,
        lidraughts.tv.Tv.emptyChampions,
        "/games",
        povOption = none,
        customTitle = if (povs.isEmpty) " - " else trans.nbGames.pluralSameTxt(povs.length)
      ).map(_.toHtml),
      moreCss = cssTags("tv.css", "form3.css"),
      moreJs = jsTag("custom-games.js")
    ) {
        div(cls := "games_playing")(
          div(cls := "game_list playing")(
            povs.map { p =>
              div(views.html.game.bits.mini(p))
            }
          )
        )
      }
}
