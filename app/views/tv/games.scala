package views.html.tv

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object games {

  def apply(channel: lila.tv.Tv.Channel, povs: List[lila.game.Pov], champions: lila.tv.Tv.Champions)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"${transKeyTxt(channel.key)} - ${trans.currentGames.txt()}",
      moreCss = cssTag("tv.games"),
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "page-menu tv-games")(
        st.aside(cls := "page-menu__menu")(
          side.channels(channel, champions, "/games")
        ),
        div(cls := "page-menu__content now-playing")(
          povs map views.html.game.bits.mini
        )
      )
    }
}
