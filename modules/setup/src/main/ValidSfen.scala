package lila.setup

import shogi.format.forsyth.Sfen

case class ValidSfen(
    sfen: Sfen,
    situation: shogi.Situation
) {

  def color = situation.color
}

object ValidSfen {
  def apply(strict: Boolean, variant: shogi.variant.Variant)(sfen: Sfen): Option[ValidSfen] =
    for {
      parsed <- sfen.toSituationPlus(variant)
      if parsed.situation.playable(strict = strict, withImpasse = true)
    } yield ValidSfen(parsed.toSfen, parsed.situation)
}
