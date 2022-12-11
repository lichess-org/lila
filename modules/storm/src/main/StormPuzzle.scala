package lila.storm

import cats.data.NonEmptyList
import chess.format.{ Fen, Uci }

import lila.puzzle.Puzzle

case class StormPuzzle(
    id: PuzzleId,
    fen: Fen.Epd,
    line: NonEmptyList[Uci.Move],
    rating: Int
):
  // ply after "initial move" when we start solving
  def initialPly: Int = ~Fen.readPly(fen)

  lazy val fenAfterInitialMove: Fen.Epd = {
    for {
      sit1 <- Fen read fen
      sit2 <- sit1.move(line.head).toOption.map(_.situationAfter)
    } yield Fen write sit2
  } err s"Can't apply puzzle $id first move"

  def color = !fen.color
