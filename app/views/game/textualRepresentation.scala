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
      dt("Turn"),
      dd(pov.game.turns),
      dt("PDN"),
      dd(role := "log", aria.live := "assertive")(raw(pov.game.pdnMoves.mkString(" "))),
      dt("FEN"),
      dd(draughts.format.Forsyth.>>(pov.game.draughts)),
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
      dd(role := "status")(
        if (pov.game.finishedOrAborted) gameEndStatus(pov.game)
        else frag(pov.game.turnColor.name, " plays")
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
