package lila.setup

import shogi.format.FEN

case class ValidFen(
    fen: FEN,
    situation: shogi.Situation
) {

  def color = situation.color
}

object ValidFen {
  def apply(strict: Boolean)(fen: String): Option[ValidFen] =
    for {
      parsed <- shogi.format.Forsyth <<< fen
      if parsed.situation playable strict
      validated = shogi.format.Forsyth >> parsed
    } yield ValidFen(FEN(validated), parsed.situation)
}
