package lila.setup

import chess.format.{ FEN, Forsyth }
import chess.variant.Chess960
import chess.variant.FromPosition
import chess.variant.Variant
import chess.{ Clock, Speed }

import lila.common.{ Days, Template }
import lila.game.{ GameRule, PerfPicker }
import lila.lobby.Color
import lila.rating.PerfType

final case class ApiConfig(
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Days],
    rated: Boolean,
    color: Color,
    position: Option[FEN] = None,
    acceptByToken: Option[String] = None,
    message: Option[Template],
    keepAliveStream: Boolean,
    rules: Set[GameRule] = Set.empty
):

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, days)

  def validFen = ApiConfig.validFen(variant, position)

  def validSpeed(isBot: Boolean) =
    !isBot || clock.fold(true) { c =>
      Speed(c) >= Speed.Bullet
    }

  def validRated = mode.casual || clock.isDefined || variant.standard

  def mode = chess.Mode(rated)

  def autoVariant =
    if (variant.standard && position.exists(!_.initial)) copy(variant = FromPosition)
    else this

object ApiConfig extends BaseHumanConfig:

  lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(_ * 60).toSet

  def from(
      v: Option[String],
      cl: Option[Clock.Config],
      d: Option[Days],
      r: Boolean,
      c: Option[String],
      pos: Option[FEN],
      tok: Option[String],
      msg: Option[String],
      keepAliveStream: Option[Boolean],
      rules: Option[Set[GameRule]]
  ) =
    new ApiConfig(
      variant = chess.variant.Variant.orDefault(~v),
      clock = cl,
      days = d,
      rated = r,
      color = Color.orDefault(~c),
      position = pos,
      acceptByToken = tok,
      message = msg map Template.apply,
      keepAliveStream = ~keepAliveStream,
      rules = ~rules
    ).autoVariant

  def validFen(variant: Variant, fen: Option[FEN]) =
    if (variant.chess960) fen.forall(f => Chess960.positionNumber(f).isDefined)
    else if (variant.fromPosition)
      fen exists { f =>
        (Forsyth <<< f).exists(_.situation playable false)
      }
    else true
