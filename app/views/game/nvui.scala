package views.html
package game

import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

import controllers.routes

object nvui {

  def json(pov: Pov)(implicit ctx: Context) = Json.obj(
    "pgn" -> pov.game.pgnMoves.mkString(" "),
    "fen" -> chess.format.Forsyth.>>(pov.game.chess),
    "status" -> povStatus(pov).toString,
    "lastMove" -> pov.game.pgnMoves.lastOption
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
      dt("PGN"),
      dd(
        cls := "pgn",
        role := "log",
        aria.live := "off",
        aria.relevant := "additions text"
      )(raw(pov.game.pgnMoves.mkString(" "))),
      dt("FEN"),
      dd(cls := "fen", aria.live := "off")(chess.format.Forsyth.>>(pov.game.chess)),
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
      )(pov.game.pgnMoves.lastOption),
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
