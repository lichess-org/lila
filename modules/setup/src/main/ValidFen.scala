package lila.setup

import chess.format.Fen

case class ValidFen(fen: Fen.Full, board: chess.Board):

  def color = board.color

object ValidFen:
  def apply(strict: Boolean)(fen: Fen.Full): Option[ValidFen] =
    for
      parsed <- chess.format.Fen.readWithMoveNumber(fen)
      if parsed.board.playable(strict)
      validated = chess.format.Fen.write(parsed)
    yield ValidFen(validated, parsed.board)
