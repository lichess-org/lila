package lila.setup

import chess.Clock
import chess.format.{ FEN, Forsyth }
import chess.variant.FromPosition

import lila.game.PerfPicker
import lila.rating.PerfType

final case class OpenConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    position: Option[FEN] = None
) {

  def >> = (variant.key.some, clock, position.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, none)

  def validFen = ApiConfig.validFen(variant, position)

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
