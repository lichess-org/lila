package views.html
package game

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object textualRepresentation {

  def apply(pov: lidraughts.game.Pov, playing: Boolean)(implicit ctx: Context) = frag(

    h1("Textual representation"),
    dl(
      if (playing) frag(
        dt("Your color"),
        dd(pov.color.name),
        dt("Opponent"),
        dd(playerLink(pov.opponent))
      )
      else frag(
        dt("White player"),
        dd(playerLink(pov.game.whitePlayer)),
        dt("Black player"),
        dd(playerLink(pov.game.blackPlayer))
      ),
      dt("PDN"),
      dd(
        cls := "pdn",
        role := "log",
        aria.live := "off",
        aria.relevant := "additions text"
      )(raw(pov.game.pdnMoves.mkString(" "))),
      dt("FEN"),
      dd(cls := "fen", aria.live := "off")(draughts.format.Forsyth.>>(pov.game.draughts)),
      dt("Game status"),
      dd(
        cls := "status",
        role := "status",
        aria.live := "assertive",
        aria.atomic := true
      )(
          if (pov.game.finishedOrAborted) gameEndStatus(pov.game)
          else frag(
            pov.game.pdnMoves.lastOption.map { lastMove =>
              s"${(!pov.game.turnColor).name} played $lastMove, "
            },
            pov.game.turnColor.name,
            " to play"
          )
        ),
      (playing && pov.game.playable) option form(
        label(
          "Your move",
          input(name := "move", cls := "move", `type` := "text", cls := "", autocomplete := "off", autofocus := true)
        )
      )
    )
  )
}
