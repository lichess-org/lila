package lila.game

import lila.rating.Perf
import lila.user.Perfs

object PerfPicker {

  val default = (perfs: Perfs) => perfs.standard

  def main(game: Game): Option[Perfs => Perf] = game.poolId match {
    case Some(id)                      => Some(_ pool id)
    case None if game.variant.standard => Perfs.speedLens(game.speed).some
    case _                             => Perfs variantLens game.variant
  }

  def mainOrDefault(game: Game): Perfs => Perf = main(game) | default
}
