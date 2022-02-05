package lila.setup

import shogi.Clock
import shogi.format.forsyth.Sfen
import lila.rating.PerfType
import lila.game.PerfPicker

final case class OpenConfig(
    variant: shogi.variant.Variant,
    clock: Option[Clock.Config],
    sfen: Option[Sfen] = None
) {

  val strictSfen = false

  def >> = (variant.key.some, clock, sfen.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(clock), variant, none)

  def validSfen =
    sfen.fold(true) { sf =>
      sf.toSituationPlus(variant).exists(_.situation.playable(strict = strictSfen, withImpasse = true))
    }

}

object OpenConfig {

  def from(v: Option[String], cl: Option[Clock.Config], sf: Option[String]) =
    new OpenConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl.filter(c => c.limitSeconds > 0 || c.hasIncrement || c.hasByoyomi),
      sfen = sf map Sfen.clean
    )
}
