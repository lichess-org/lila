package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object textualRepresentation {

  def apply(pov: lila.game.Pov, playing: Boolean)(implicit ctx: Context) = frag(

    h1("Textual representation"),
    dl(
      dt("Turn"),
      dd(pov.game.turns),
      dt("PGN"),
      dd(role := "log", aria.live := "assertive")(raw(pov.game.pgnMoves.mkString(" "))),
      dt("FEN"),
      dd(chess.format.Forsyth.>>(pov.game.chess)),
      if (playing) frag(
        dt("Your color"),
        dd(pov.color.name),
        dt("Opponent"),
        dd(playerText(pov.opponent))
      )
      else frag(
        dt("White player"),
        dd(playerText(pov.game.whitePlayer)),
        dt("Black player"),
        dd(playerText(pov.game.blackPlayer))
      ),
      dt("Game status"),
      dd(role := "status", aria.live := "assertive")(
        if (pov.game.finishedOrAborted) gameEndStatus(pov.game)
        else frag(
          pov.game.pgnMoves.lastOption.map { lastMove =>
            s"${(!pov.game.turnColor).name} played $lastMove, "
          },
          pov.game.turnColor.name,
          " to play"
        )
      ),
      (playing && pov.game.playable && pov.game.turnOf(pov.player)) option form(
        label(
          "Your move",
          input(name := "move", cls := "move", `type` := "text", cls := "", autocomplete := "off", autofocus := true)
        ),
        button(`type` := "submit")("Send move")
      )
    )
  )
}
