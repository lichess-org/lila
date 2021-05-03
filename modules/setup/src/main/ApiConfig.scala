package lila.setup

import shogi.Clock
import shogi.format.{ FEN, Forsyth }
import shogi.variant.FromPosition
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker

final case class ApiConfig(
    variant: shogi.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Int],
    rated: Boolean,
    color: Color,
    position: Option[FEN] = None,
    acceptByToken: Option[String] = None
) {

  val strictFen = false

  def >> = (variant.key.some, clock, days, rated, color.name.some, position.map(_.value), acceptByToken).some

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(clock), variant, days)

  def validFen =
    variant != FromPosition || {
      position ?? { f =>
        ~(Forsyth <<< f.value).map(_.situation playable strictFen)
      }
    }

  def mode = shogi.Mode(rated)

  def autoVariant =
    if (variant.standard && position.exists(_.value != Forsyth.initial)) copy(variant = FromPosition)
    else this
}

object ApiConfig extends BaseHumanConfig {

  lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(60 *).toSet

  def from(
      v: Option[String],
      cl: Option[Clock.Config],
      d: Option[Int],
      r: Boolean,
      c: Option[String],
      pos: Option[String],
      tok: Option[String]
  ) =
    new ApiConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl,
      days = d,
      rated = r,
      color = Color.orDefault(~c),
      position = pos map FEN,
      acceptByToken = tok
    ).autoVariant
}
