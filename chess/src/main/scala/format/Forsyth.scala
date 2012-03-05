package lila.chess
package format

import Pos.posAt

/**
 * Transform a game to standard Forsyth Edwards Notation
 * http://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
 */
object Forsyth extends Format[Game] {

  def <<(source: String): Game = {
    val useful = """\s*([\w\d/]+)\s.+""".r.replaceAllIn(
      source.replace("/", ""),
      m ⇒ m group 1
    )
    Game()
  }

  def >>(game: Game): String = List(
    exportBoard(game.board),
    game.player.letter,
    game.board.history.castleNotation,
    ((for {
      lastMove ← game.board.history.lastMove
      (orig, dest) = lastMove
      piece ← game board dest
      if piece is Pawn
      pos ← if (orig.y == 2 && dest.y == 4) dest.down
      else if (orig.y == 7 && dest.y == 5) dest.up
      else None
    } yield pos.toString) getOrElse "-"),
    game.halfMoveClock,
    game.fullMoveNumber
  ) mkString " "

  private def exportBoard(board: Board) = {
    {
      for (y ← 8 to 1 by -1) yield {
        (1 to 8).map(board(_, y)).foldLeft(("", 0)) {
          case ((out, empty), None)        ⇒ (out, empty + 1)
          case ((out, 0), Some(piece))     ⇒ (out + piece.forsyth.toString, 0)
          case ((out, empty), Some(piece)) ⇒ (out + empty.toString + piece.forsyth, 0)
        } match {
          case (out, 0)     ⇒ out
          case (out, empty) ⇒ out + empty
        }
      } mkString
    } mkString "/"
  } mkString
}
