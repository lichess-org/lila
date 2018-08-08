package lidraughts.setup

case class ValidFen(
    fen: String,
    situation: draughts.Situation
) {

  def color = situation.color
}

object ValidFen {
  def apply(strict: Boolean)(fen: String): Option[ValidFen] = for {
    parsed ← draughts.format.Forsyth <<< fen
    if (parsed.situation playable strict)
    validated = draughts.format.Forsyth >> parsed
  } yield ValidFen(validated, parsed.situation)
}
