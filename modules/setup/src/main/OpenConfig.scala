package lila.setup

import chess.Clock
import chess.format.FEN
import chess.variant.FromPosition

import lila.game.PerfPicker
import lila.rating.PerfType

final case class OpenConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    rated: Boolean,
    position: Option[FEN] = None
) {

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, none)

  def validFen = ApiConfig.validFen(variant, position)

  def autoVariant =
    if (variant.standard && position.exists(!_.initial)) copy(variant = FromPosition)
    else this
}

object OpenConfig {

  def from(v: Option[String], cl: Option[Clock.Config], rated: Boolean, pos: Option[String]) =
    new OpenConfig(
      variant = chess.variant.Variant.orDefault(~v),
      clock = cl,
      rated = rated,
      position = pos map FEN.apply
    ).autoVariant
}
