package lila.game

import chess.{ Variant, Speed }
import lila.rating.{ Perf, PerfType }
import lila.user.Perfs

object PerfPicker {

  val default = (perfs: Perfs) => perfs.standard

  def perfType(speed: Speed, variant: Variant): Option[PerfType] =
    PerfType(key(speed, variant))

  def key(speed: Speed, variant: Variant): String =
    if (variant.standard) {
      if (speed == Speed.Unlimited) Speed.Classical.key
      else speed.key
    }
    else variant.key

  def key(game: Game): String = key(game.speed, game.variant)

  def main(speed: Speed, variant: Variant): Option[Perfs => Perf] =
    if (variant.standard) Perfs.speedLens(speed).some
    else Perfs variantLens variant

  def main(game: Game): Option[Perfs => Perf] = main(game.speed, game.variant)

  def mainOrDefault(game: Game): Perfs => Perf = main(game) | default

  def mainOrDefault(speed: Speed, variant: Variant): Perfs => Perf =
    main(speed, variant) | default
}
