package views.html.tv

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object games {

  def apply(channel: lila.tv.Tv.Channel, povs: List[lila.game.Pov], champions: lila.tv.Tv.Champions)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"${channel.name} • ${trans.currentGames.txt()}",
      moreCss = cssTag("tv.games"),
      moreJs = jsModule("tvGames")
    ) {
      main(
        cls     := "page-menu tv-games",
        dataRel := s"$netBaseUrl${routes.Tv.gameChannelReplacement(channel.key, "gameId", Nil)}"
      )(
        st.aside(cls := "page-menu__menu")(
          side.channels(channel, champions, "/games")
        ),
        div(cls := "page-menu__content now-playing")(
          povs map { views.html.game.mini(_) }
        )
      )
    }
}
