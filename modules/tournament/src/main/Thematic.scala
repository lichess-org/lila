package lila.tournament

import shogi.Handicap
import shogi.format.forsyth.Sfen

object Thematic {

  def bySfen(sfen: Sfen, variant: shogi.variant.Variant): Option[Handicap] =
    sfenIndex.get(variant).flatMap { _.get(sfen.value) }

  private lazy val sfenIndex: Map[shogi.variant.Variant, Map[String, Handicap]] =
    Handicap.allByVariant.view.map { case (v, hs) =>
      v -> hs.map(h => h.sfen.value -> h).toMap
    }.toMap

}
