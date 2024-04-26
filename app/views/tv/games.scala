package views.tv

import lila.app.templating.Environment.{ *, given }

object games:

  def apply(channel: lila.tv.Tv.Channel, povs: List[Pov], champions: lila.tv.Tv.Champions)(using
      ctx: PageContext
  ) =
    views.base.layout(
      title = s"${channel.name} • ${trans.site.currentGames.txt()}",
      moreCss = cssTag("tv.games"),
      modules = jsModule("bits.tvGames")
    ) {
      main(
        cls     := "page-menu tv-games",
        dataRel := s"$netBaseUrl${routes.Tv.gameChannelReplacement(channel.key, "gameId", Nil)}"
      )(
        st.aside(cls := "page-menu__menu")(
          side.channels(channel, champions, "/games")
        ),
        div(cls := "page-menu__content now-playing")(
          povs.map { views.game.mini(_) }
        )
      )
    }
