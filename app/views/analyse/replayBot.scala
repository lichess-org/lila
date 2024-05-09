package views.analyse

import lila.app.UiEnv.{ *, given }

def replayBot(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    pgn: String,
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup]
)(using Context) =
  Page(ui.titleOf(pov))
    .css("analyse.round")
    .graph(views.round.ui.povOpenGraph(pov))
    .csp(bits.csp)
    .robots(false):
      main(cls := "analyse")(
        st.aside(cls := "analyse__side")(
          views.game.side(pov, initialFen, none, simul = simul, bookmarked = false)
        ),
        div(cls := "analyse__board main-board")(chessgroundBoard),
        div(cls := "analyse__tools")(div(cls := "ceval")),
        div(cls := "analyse__controls"),
        div(cls := "analyse__underboard")(
          div(cls := "analyse__underboard__panels")(
            div(cls := "fen-pgn active")(
              div(
                strong("FEN"),
                input(readonly, spellcheck := false, cls := "analyse__underboard__fen")
              ),
              div(cls := "pgn")(pgn)
            ),
            cross.map: c =>
              div(cls := "ctable active")(
                views.game.ui.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
              )
          )
        )
      )
