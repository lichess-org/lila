package lila.setup

import chess.{ Mode, Speed }
import lila.rating.RatingRange

case class FilterConfig(
    variant: List[chess.variant.Variant],
    mode: List[Mode],
    speed: List[Speed],
    increment: List[FilterConfig.Increment],
    ratingRange: RatingRange
) {

  def >> =
    (
      variant map (_.id),
      mode map (_.id),
      speed map (_.id),
      increment map FilterConfig.Increment.iso.to,
      ratingRange.toString
    ).some

  def nonEmpty =
    copy(
      variant = if (variant.isEmpty) FilterConfig.default.variant else variant,
      mode = if (mode.isEmpty) FilterConfig.default.mode else mode,
      increment = if (increment.isEmpty) FilterConfig.default.increment else increment,
      speed = if (speed.isEmpty) FilterConfig.default.speed else speed
    )
}

object FilterConfig {

  sealed trait Increment
  object Increment {
    case object Yes extends Increment
    case object No  extends Increment
    val iso = lila.common.Iso[Int, Increment](
      i => if (i == 0) No else Yes,
      i => if (i == No) 0 else 1
    )
  }

  val variants = List(
    chess.variant.Standard,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.Antichess,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.RacingKings,
    chess.variant.Crazyhouse
  )

  val default = FilterConfig(
    variant = variants,
    mode = Mode.all,
    speed = Speed.all,
    increment = List(Increment.Yes, Increment.No),
    ratingRange = RatingRange.default
  )

  def <<(v: List[Int], m: List[Int], s: List[Int], i: List[Int], e: String) =
    new FilterConfig(
      variant = v flatMap chess.variant.Variant.apply,
      mode = m flatMap Mode.apply,
      speed = s flatMap Speed.apply,
      increment = i map Increment.iso.from,
      ratingRange = RatingRange orDefault e
    ).nonEmpty
}
