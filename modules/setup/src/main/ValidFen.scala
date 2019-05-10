package lidraughts.setup

import draughts.format.Forsyth

case class ValidFen(
    fen: String,
    situation: draughts.Situation
) {

  def color = situation.color
  def tooManyKings = Forsyth.countKings(fen) > 30
}

object ValidFen {
  def apply(strict: Boolean)(fen: String): Option[ValidFen] = for {
    parsed ← Forsyth <<< fen
    if (parsed.situation playable strict)
    validated = Forsyth >> parsed
  } yield ValidFen(validated, parsed.situation)
}
