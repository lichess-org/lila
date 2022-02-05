package lila.setup

import shogi.format.forsyth.Sfen

case class ValidSfen(
    sfen: Sfen,
    situation: shogi.Situation
) {

  def color = situation.color
}

object ValidSfen {
  def apply(strict: Boolean)(sfen: Sfen): Option[ValidSfen] =
    for {
      parsed <- sfen.toSituationPlus(shogi.variant.Standard)
      if parsed.situation.playable(strict = strict, withImpasse = true)
      validated = parsed.toSfen
    } yield ValidSfen(validated, parsed.situation)
}
