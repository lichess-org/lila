package lila.setup

import shogi.Clock
import shogi.format.{ FEN, Forsyth }
import shogi.variant.FromPosition
import lila.rating.PerfType
import lila.game.PerfPicker

final case class OpenConfig(
    variant: shogi.variant.Variant,
    clock: Option[Clock.Config],
    position: Option[FEN] = None
) {

  val strictFen = false

  def >> = (variant.key.some, clock, position.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(clock), variant, none)

  def validFen =
    variant != FromPosition || {
      position ?? { f =>
        ~(Forsyth <<< f.value).map(_.situation playable strictFen)
      }
    }

  def autoVariant =
    if (variant.standard && position.exists(_.value != Forsyth.initial)) copy(variant = FromPosition)
    else this
}

object OpenConfig {

  def from(v: Option[String], cl: Option[Clock.Config], pos: Option[String]) =
    new OpenConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl,
      position = pos map FEN
    ).autoVariant
}
