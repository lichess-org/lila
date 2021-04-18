package lila.storm

import scalaz.NonEmptyList
import chess.format.{ FEN, Forsyth, Uci }

import lila.puzzle.Puzzle

case class StormPuzzle(
    id: Puzzle.Id,
    fen: String,
    line: NonEmptyList[Uci.Move],
    rating: Int
) {
  // ply after "initial move" when we start solving
  def initialPly: Int =
      fen.split(' ').lift(3).flatMap(_.toIntOption) ?? { move =>
        move
    }

  lazy val fenAfterInitialMove: FEN = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(line.head).toOption.map(_.situationAfter)
    } yield FEN(Forsyth >> sit2)
  } err s"Can't apply puzzle $id first move"

  def color = Forsyth.getColor(fen).fold[chess.Color](chess.Sente)(!_)
}
