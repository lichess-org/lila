package lila.setup

import scala.collection.breakOut

import chess.Clock
import chess.format.{ FEN, Forsyth }
import chess.variant.FromPosition
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker

case class ApiConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Int],
    rated: Boolean,
    color: Color,
    position: Option[FEN] = None
) extends {

  val strictFen = false

  def >> = (variant.key.some, clock, days, rated, color.name.some, position.map(_.value)).some

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, days)

  def validFen = variant != FromPosition || {
    position ?? { f => ~(Forsyth <<< f.value).map(_.situation playable strictFen) }
  }

  def mode = chess.Mode(rated)
}

object ApiConfig extends BaseHumanConfig {

  lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).map(60*)(breakOut)

  def <<(v: Option[String], cl: Option[Clock.Config], d: Option[Int], r: Boolean, c: Option[String], pos: Option[String]) =
    new ApiConfig(
      variant = chess.variant.Variant.orDefault(~v),
      clock = cl,
      days = d,
      rated = r,
      color = Color.orDefault(~c),
      position = pos map FEN
    )
}
