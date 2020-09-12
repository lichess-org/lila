package lila.setup

import chess.format.FEN

case class ValidFen(
    fen: FEN,
    situation: chess.Situation
) {

  def color = situation.color
}

object ValidFen {
  def apply(strict: Boolean)(fen: String): Option[ValidFen] =
    for {
      parsed <- chess.format.Forsyth <<< fen
      if parsed.situation playable strict
      validated = chess.format.Forsyth >> parsed
    } yield ValidFen(FEN(validated), parsed.situation)
}
