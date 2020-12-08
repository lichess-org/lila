package views.html.analyse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

object replayBot {

  def apply(
      pov: Pov,
      initialFen: Option[chess.format.FEN],
      pgn: String,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) = {

    views.html.analyse.bits.layout(
      title = replay titleOf pov,
      //moreCss = cssTag("analyse.round"),
      moreCss = frag(cssTag("analyse.round"), cssTag("analyse.zh")),
      openGraph = povOpenGraph(pov).some
    ) {
      main(cls := "analyse")(
        st.aside(cls := "analyse__side")(
          views.html.game.side(pov, initialFen, none, simul = simul, bookmarked = false)
        ),
        div(cls := "analyse__board main-board")(shogigroundBoard),
        div(cls := "analyse__tools")(div(cls := "ceval")),
        div(cls := "analyse__controls"),
        div(cls := "analyse__underboard")(
          div(cls := "analyse__underboard__panels")(
            div(cls := "fen-pgn active")(
              div(
                strong("SFEN"),
                input(readonly, spellcheck := false, cls := "copyable autoselect analyse__underboard__fen")
              ),
              div(cls := "pgn")(pgn)
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
