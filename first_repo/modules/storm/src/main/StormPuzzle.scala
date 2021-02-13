package lila.storm

import cats.data.NonEmptyList
import chess.format.{ FEN, Forsyth, Uci }

import lila.puzzle.Puzzle

case class StormPuzzle(
    id: Puzzle.Id,
    fen: FEN,
    line: NonEmptyList[Uci.Move],
    rating: Int
) {
  // ply after "initial move" when we start solving
  def initialPly: Int =
    fen.fullMove ?? { fm =>
      fm * 2 - color.fold(1, 2)
    }

  lazy val fenAfterInitialMove: FEN = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(line.head).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
  } err s"Can't apply puzzle $id first move"

  def color = fen.color.fold[chess.Color](chess.White)(!_)
}
