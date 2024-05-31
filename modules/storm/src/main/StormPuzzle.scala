package lila.storm

import cats.data.NonEmptyList
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

import lila.puzzle.Puzzle

// Only tsume puzzles that are NOT from games
case class StormPuzzle(
    id: Puzzle.Id,
    sfen: Sfen,
    line: NonEmptyList[Usi],
    rating: Int
) {
  // ply after "initial move" when we start solving
  def initialPly: Int =
    sfen.stepNumber ?? { mn =>
      mn - 1
    }

  lazy val sfenAfterInitialMove: Sfen = sfen

  def color: shogi.Color =
    sfen.color.getOrElse(shogi.Sente)
}
