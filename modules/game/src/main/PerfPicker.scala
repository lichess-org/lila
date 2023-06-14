package lila.game

import chess.Speed
import lila.rating.{ Perf, PerfType }
import lila.user.Perfs
import lila.common.Days

object PerfPicker:

  val default = (perfs: Perfs) => perfs.standard

  def perfType(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Option[PerfType] =
    PerfType(key(speed, variant, daysPerTurn))

  def key(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Perf.Key =
    if (variant.standard)
      if (daysPerTurn.isDefined || speed == Speed.Correspondence) PerfType.Correspondence.key
      else speed.key into Perf.Key
    else variant.key into Perf.Key

  def key(game: Game): Perf.Key = key(game.speed, game.ratingVariant, game.daysPerTurn)

  def main(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Option[Perfs => Perf] =
    if (variant.standard) Some {
      if (daysPerTurn.isDefined) (perfs: Perfs) => perfs.correspondence
      else Perfs speedLens speed
    }
    else Perfs variantLens variant

  def main(game: Game): Option[Perfs => Perf] = main(game.speed, game.ratingVariant, game.daysPerTurn)

  def mainOrDefault(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Perfs => Perf =
    main(speed, variant, daysPerTurn) orElse {
      (variant == chess.variant.FromPosition) so main(speed, chess.variant.Standard, daysPerTurn)
    } getOrElse default

  def mainOrDefault(game: Game): Perfs => Perf =
    mainOrDefault(game.speed, game.ratingVariant, game.daysPerTurn)
