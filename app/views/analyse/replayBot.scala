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
      main(cls := s"analyse variant-${pov.game.variant.key}")(
        st.aside(cls := "analyse__side")(
          views.html.game.side(pov, none, simul = simul, bookmarked = false)
        ),
        div(cls := "analyse__board main-board")(shogigroundBoard(pov.game.variant, pov.color.some)),
        (!pov.game.variant.chushogi) option sgHandTop,
        div(cls := "analyse__tools")(div(cls := "ceval")),
        (!pov.game.variant.chushogi) option sgHandBottom,
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
        )
      )
    }
  }
}
