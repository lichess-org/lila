package lila.setup

import shogi.Clock
import shogi.format.forsyth.Sfen
import shogi.variant.FromPosition
import lila.rating.PerfType
import lila.game.PerfPicker

final case class OpenConfig(
    variant: shogi.variant.Variant,
    clock: Option[Clock.Config],
    position: Option[Sfen] = None
) {

  val strictSfen = false

  def >> = (variant.key.some, clock, position.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(clock), variant, none)

  def validSfen =
    variant != FromPosition || {
      position ?? { f =>
        ~(f.toSituationPlus(shogi.variant.Standard)).map(_.situation.playable(strict = strictSfen, withImpasse = true))
      }
    }

  def autoVariant =
    if (variant.standard && position.exists(!_.initialOf(variant))) copy(variant = FromPosition)
    else this
}

object OpenConfig {

  def from(v: Option[String], cl: Option[Clock.Config], pos: Option[String]) =
    new OpenConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl.filter(c => c.limitSeconds > 0 || c.hasIncrement || c.hasByoyomi),
      position = pos map Sfen.apply
    ).autoVariant
}
