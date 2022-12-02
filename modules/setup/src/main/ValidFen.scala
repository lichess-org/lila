package lila.setup

import chess.format.Fen

case class ValidFen(fen: Fen, situation: chess.Situation):

  def color = situation.color

object ValidFen:
  def apply(strict: Boolean)(fen: Fen): Option[ValidFen] =
    for {
      parsed <- chess.format.Fen readWithMoveNumber fen
      if parsed.situation playable strict
      validated = chess.format.Fen write parsed
    } yield ValidFen(validated, parsed.situation)
