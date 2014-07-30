package lila.game

import chess.{ Variant, Speed }
import lila.rating.Perf
import lila.user.Perfs

object PerfPicker {

  val default = (perfs: Perfs) => perfs.standard

  def key(speed: Speed, variant: Variant, poolId: Option[String]): String =
    poolId match {
      case Some(_)                  => "pool"
      case None if variant.standard => speed.key
      case _                        => variant.key
    }

  def main(speed: Speed, variant: Variant, poolId: Option[String]): Option[Perfs => Perf] =
    poolId match {
      case Some(id)                 => Some(_ pool id)
      case None if variant.standard => Perfs.speedLens(speed).some
      case _                        => Perfs variantLens variant
    }

  def main(game: Game): Option[Perfs => Perf] = main(game.speed, game.variant, game.poolId)

  def mainOrDefault(game: Game): Perfs => Perf = main(game) | default
}
