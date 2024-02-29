package views.html.analyse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

object replayBot {

  def apply(
      pov: Pov,
      kif: String,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) = {
    views.html.analyse.bits.layout(
      title = replay titleOf pov,
      moreCss = cssTag("analyse.round"),
      openGraph = povOpenGraph(pov).some
    ) {
      main(cls := s"analyse ${mainVariantClass(pov.game.variant)}")(
        st.aside(cls := "analyse__side")(
          views.html.game.side(pov, none, simul = simul, bookmarked = false)
        ),
        div(cls := s"analyse__board main-board ${variantClass(pov.game.variant)}")(
          shogigroundEmpty(pov.game.variant, pov.color)
        ),
        div(cls := "analyse__tools")(div(cls := "ceval")),
        div(cls := "analyse__controls"),
        div(cls := "analyse__underboard")(
          div(cls := "analyse__underboard__panels")(
            div(cls := "sfen-notation active")(
              div(
                strong("SFEN"),
                input(readonly, spellcheck := false, cls := "copyable autoselect analyse__underboard__sfen")
              ),
              div(cls := "kif")(kif)
            ),
            cross.map { c =>
              div(cls := "ctable active")(
                views.html.game.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
              )
            }
          )
        ),
        div(cls := "analyse__acpl")
      )
    }
  }
}
