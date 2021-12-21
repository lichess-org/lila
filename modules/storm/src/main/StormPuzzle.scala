package lila.storm

import cats.data.NonEmptyList
import shogi.format.{ FEN, Forsyth, Usi }

import lila.puzzle.Puzzle

// Only tsume puzzles that are NOT from games
case class StormPuzzle(
    id: Puzzle.Id,
    fen: String,
    line: NonEmptyList[Usi],
    rating: Int
) {
  // ply after "initial move" when we start solving
  def initialPly: Int = {
    fen.split(' ').lift(3).flatMap(_.toIntOption) ?? { move =>
      move - 1
    }
  }

  lazy val fenAfterInitialMove: FEN = FEN(fen)

  def color: shogi.Color =
    Forsyth.getColor(fen).getOrElse(shogi.Sente)
}
