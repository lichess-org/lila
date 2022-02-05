package lila.setup

import shogi.Clock
import shogi.format.forsyth.Sfen
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker

final case class ApiConfig(
    variant: shogi.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Int],
    rated: Boolean,
    color: Color,
    sfen: Option[Sfen] = None,
    acceptByToken: Option[String] = None
) {

  val strictSfen = false

  def >> = (variant.key.some, clock, days, rated, color.name.some, sfen.map(_.value), acceptByToken).some

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(clock), variant, days)

  def validSfen =
    sfen.fold(true) { sf =>
      sf.toSituationPlus(variant).exists(_.situation.playable(strict = strictSfen, withImpasse = true))
    }

  def mode = shogi.Mode(rated)

}

object ApiConfig extends BaseHumanConfig {

  lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(60 *).toSet

  def from(
      v: Option[String],
      cl: Option[Clock.Config],
      d: Option[Int],
      r: Boolean,
      c: Option[String],
      sf: Option[String],
      tok: Option[String]
  ) =
    new ApiConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl.filter(c => c.limitSeconds > 0 || c.hasIncrement || c.hasByoyomi),
      days = d,
      rated = r,
      color = Color.orDefault(~c),
      sfen = sf map Sfen.apply,
      acceptByToken = tok
    )
}
