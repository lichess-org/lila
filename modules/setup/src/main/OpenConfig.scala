package lila.setup

import chess.Clock
import chess.format.{ FEN, Forsyth }
import chess.variant.FromPosition
import lila.rating.PerfType
import lila.game.PerfPicker

final case class OpenConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    position: Option[FEN] = None
) {

  val strictFen = false

  def >> = (variant.key.some, clock, position.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, none)

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
      variant = chess.variant.Variant.orDefault(~v),
      clock = cl,
      position = pos map FEN
    ).autoVariant
}
