package views.html
package game

import play.api.libs.json._

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.Pov

import controllers.routes

object nvui {

  def json(pov: Pov)(implicit ctx: Context) = Json.obj(
    "pdn" -> pov.game.pdnMoves.mkString(" "),
    "fen" -> draughts.format.Forsyth.>>(pov.game.draughts),
    "status" -> povStatus(pov).toString,
    "lastMove" -> pov.game.pdnMoves.lastOption
  )

  def html(pov: Pov, playing: Boolean)(implicit ctx: Context) = frag(
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
      )(povStatus(pov)),
      dt("Last move"),
      dd(
        cls := "lastMove",
        aria.live := "assertive",
        aria.atomic := true
      )(pov.game.pdnMoves.lastOption),
      (playing && pov.game.playable) option form(
        label(
          "Your move",
          input(name := "move", cls := "move", `type` := "text", cls := "", autocomplete := "off", autofocus := true)
        )
      ),
      div(cls := "notify", aria.live := "assertive", aria.atomic := true)
    )
  )

  private def povStatus(pov: Pov)(implicit ctx: Context) =
    if (pov.game.finishedOrAborted) gameEndStatus(pov.game)
    else "playing"
}
