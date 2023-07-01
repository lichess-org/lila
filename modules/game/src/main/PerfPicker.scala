package lila.game

import chess.Speed
import lila.rating.{ Perf, PerfType }
import lila.user.UserPerfs
import lila.common.Days

object PerfPicker:

  val default = (perfs: UserPerfs) => perfs.standard

  def perfType(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Option[PerfType] =
    PerfType(key(speed, variant, daysPerTurn))

  def key(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Days]): Perf.Key =
    if variant.standard then
      if daysPerTurn.isDefined || speed == Speed.Correspondence
      then PerfType.Correspondence.key
      else speed.key into Perf.Key
    else variant.key into Perf.Key

  def key(game: Game): Perf.Key = key(game.speed, game.ratingVariant, game.daysPerTurn)

  def main(
      speed: Speed,
      variant: chess.variant.Variant,
      daysPerTurn: Option[Days]
  ): Option[UserPerfs => Perf] =
    if variant.standard then
      Some:
        if (daysPerTurn.isDefined) (perfs: UserPerfs) => perfs.correspondence
        else UserPerfs speedLens speed
    else UserPerfs variantLens variant

  def main(game: Game): Option[UserPerfs => Perf] = main(game.speed, game.ratingVariant, game.daysPerTurn)

  def mainOrDefault(
      speed: Speed,
      variant: chess.variant.Variant,
      daysPerTurn: Option[Days]
  ): UserPerfs => Perf =
    main(speed, variant, daysPerTurn) orElse {
      (variant == chess.variant.FromPosition) so main(speed, chess.variant.Standard, daysPerTurn)
    } getOrElse default

  def mainOrDefault(game: Game): UserPerfs => Perf =
    mainOrDefault(game.speed, game.ratingVariant, game.daysPerTurn)
