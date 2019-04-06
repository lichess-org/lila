package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object games {

  def apply(channel: lidraughts.tv.Tv.Channel, povs: List[lidraughts.game.Pov], champions: lidraughts.tv.Tv.Champions)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${channel.name} • ${trans.currentGames.txt()}",
      moreCss = responsiveCssTag("tv.games"),
      responsive = true
    ) {
        main(cls := "page-menu tv-games")(
          st.aside(cls := "page-menu__menu")(
            side(channel, champions, "/games", povOption = none)
          ),
          div(cls := "page-menu__content now-playing")(
            povs.map { p =>
              a(href := gameLink(p))(views.html.game.bits.mini(p))
            }
          )
        )
      }
}
