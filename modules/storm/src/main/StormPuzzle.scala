package lila.storm

import chess.format.{ Fen, Uci }
import chess.IntRating

import lila.core.id.PuzzleId

case class StormPuzzle(
    id: PuzzleId,
    fen: Fen.Full,
    line: NonEmptyList[Uci.Move],
    rating: IntRating
):
  // ply after "initial move" when we start solving
  def initialPly = Fen.readPly(fen) | chess.Ply.initial

  lazy val fenAfterInitialMove: Fen.Full = {
    for
      p1 <- Fen.read(fen)
      p2 <- p1.move(line.head).toOption.map(_.after)
    yield Fen.write(p2)
  }.err(s"Can't apply puzzle $id first move")

  def color = !fen.colorOrWhite
