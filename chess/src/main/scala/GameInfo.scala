package lila.chess

import format.PgnDump
import Eco.Opening

final class GameInfo private (
    val game: Game,
    val pgn: String,
    val opening: Option[Opening]) {

  def toMap = Map(
    "pgn" -> pgn,
    "opening" -> (opening map { o ⇒
      Map(
        "code" -> o.code,
        "name" -> o.name
      )
    })
  )
}

object GameInfo {

  def apply(game: Game) = new GameInfo(
    game = game,
    pgn = game.pgnMoves,
    opening = Eco openingOf game.pgnMoves)
}
