package lila.setup

case class ValidFen(
    fen: String,
    situation: chess.Situation
) {

  def color = situation.color
}

object ValidFen {
  def apply(strict: Boolean)(fen: String): Option[ValidFen] = for {
    parsed ← chess.format.Forsyth <<< fen
    if (parsed.situation playable strict)
    validated = chess.format.Forsyth >> parsed
  } yield ValidFen(validated, parsed.situation)
}
