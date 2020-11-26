package lila.setup

import chess.format.FEN

case class ValidFen(fen: FEN, situation: chess.Situation) {

  def color = situation.color
}

object ValidFen {
  def apply(strict: Boolean)(fen: FEN): Option[ValidFen] =
    for {
      parsed <- chess.format.Forsyth <<< fen
      if parsed.situation playable strict
      validated = chess.format.Forsyth >> parsed
    } yield ValidFen(validated, parsed.situation)
}
