package lila.setup

import chess.format.Fen
import chess.variant.{ Chess960, Variant, FromPosition }
import chess.{ Clock, Speed }

import lila.common.{ Days, Template }
import lila.game.GameRule
import lila.lobby.Color
import lila.rating.PerfType

final case class ApiConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Days],
    rated: Boolean,
    color: Color,
    position: Option[Fen.Epd] = None,
    message: Option[Template],
    keepAliveStream: Boolean,
    rules: Set[GameRule] = Set.empty
):

  def perfType: PerfType = PerfType(variant, chess.Speed(days.isEmpty so clock))

  def validFen = ApiConfig.validFen(variant, position)

  def validSpeed(isBot: Boolean) =
    !isBot || clock.forall: c =>
      Speed(c) >= Speed.Bullet

  def validRated = mode.casual || clock.isDefined || variant.standard

  def mode = chess.Mode(rated)

  def autoVariant =
    if variant.standard && position.exists(!_.isInitial)
    then copy(variant = FromPosition)
    else this

object ApiConfig extends BaseHumanConfig:

  lazy val clockLimitSeconds =
    Clock.LimitSeconds.from(Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(_ * 60).toSet)

  def from(
      v: Option[Variant.LilaKey],
      cl: Option[Clock.Config],
      d: Option[Days],
      r: Boolean,
      c: Option[String],
      pos: Option[Fen.Epd],
      msg: Option[String],
      keepAliveStream: Option[Boolean],
      rules: Option[Set[GameRule]]
  ) =
    ApiConfig(
      variant = chess.variant.Variant.orDefault(v),
      clock = cl,
      days = d,
      rated = r,
      color = Color.orDefault(~c),
      position = pos,
      message = msg map Template.apply,
      keepAliveStream = ~keepAliveStream,
      rules = ~rules
    ).autoVariant

  def validFen(variant: Variant, fen: Option[Fen.Epd]) =
    if variant.chess960
    then fen.forall(f => Chess960.positionNumber(f).isDefined)
    else if variant.fromPosition then
      fen.exists: f =>
        Fen.read(f).exists(_ playable false)
    else true
