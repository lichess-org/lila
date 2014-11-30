package lila.game

import chess.{ Variant, Speed }
import lila.rating.{ Perf, PerfType }
import lila.user.Perfs

object PerfPicker {

  val default = (perfs: Perfs) => perfs.standard

  def perfType(speed: Speed, variant: Variant, daysPerTurn: Option[Int]): Option[PerfType] =
    PerfType(key(speed, variant, daysPerTurn))

  def key(speed: Speed, variant: Variant, daysPerTurn: Option[Int]): String =
    if (variant.standard) {
      if (daysPerTurn.isDefined) PerfType.Correspondence.key
      else if (speed == Speed.Unlimited) PerfType.Classical.key
      else speed.key
    }
    else variant.key

  def key(game: Game): String = key(game.speed, game.variant, game.daysPerTurn)

  def main(speed: Speed, variant: Variant, daysPerTurn: Option[Int]): Option[Perfs => Perf] =
    if (daysPerTurn.isDefined) Some((perfs: Perfs) => perfs.correspondence)
    else if (variant.standard) Perfs.speedLens(speed).some
    else Perfs variantLens variant

  def main(game: Game): Option[Perfs => Perf] = main(game.speed, game.variant, game.daysPerTurn)

  def mainOrDefault(game: Game): Perfs => Perf = main(game) | default

  def mainOrDefault(speed: Speed, variant: Variant, daysPerTurn: Option[Int]): Perfs => Perf =
    main(speed, variant, daysPerTurn) | default
}
