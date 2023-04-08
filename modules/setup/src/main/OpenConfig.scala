package lila.setup

import chess.Clock
import chess.format.Fen
import chess.variant.{ Variant, FromPosition }

import lila.common.Days
import lila.game.{ GameRule, PerfPicker }
import lila.rating.PerfType

final case class OpenConfig(
    name: Option[String],
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Days],
    rated: Boolean,
    position: Option[Fen.Epd],
    userIds: Option[(UserId, UserId)],
    rules: Set[GameRule] = Set.empty
):

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(clock), variant, none)

  def validFen = ApiConfig.validFen(variant, position)

  def autoVariant =
    if (variant.standard && position.exists(!_.isInitial)) copy(variant = FromPosition)
    else this

object OpenConfig:

  def from(
      n: Option[String],
      v: Option[Variant.LilaKey],
      cl: Option[Clock.Config],
      days: Option[Days],
      rated: Boolean,
      pos: Option[Fen.Epd],
      usernames: Option[List[UserStr]],
      rules: Option[Set[GameRule]]
  ) =
    new OpenConfig(
      name = n.map(_.trim).filter(_.nonEmpty),
      variant = Variant.orDefault(v),
      clock = cl,
      days = days,
      rated = rated,
      position = pos,
      userIds = usernames.map(_.map(_.id)) collect { case List(w, b) =>
        (w, b)
      },
      rules = ~rules
    ).autoVariant
