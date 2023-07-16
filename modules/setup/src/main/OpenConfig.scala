package lila.setup

import chess.Clock
import chess.format.Fen
import chess.variant.{ Variant, FromPosition }

import lila.common.Days
import lila.game.GameRule
import lila.rating.PerfType

final case class OpenConfig(
    name: Option[String],
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Days],
    rated: Boolean,
    position: Option[Fen.Epd],
    userIds: Option[(UserId, UserId)],
    rules: Set[GameRule] = Set.empty,
    expiresAt: Option[Instant]
):

  def perfType = PerfType(variant, chess.Speed(clock))

  def validFen = ApiConfig.validFen(variant, position)

  def autoVariant =
    if variant.standard && position.exists(!_.isInitial)
    then copy(variant = FromPosition)
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
      rules: Option[Set[GameRule]],
      expiresAt: Option[Instant]
  ) =
    OpenConfig(
      name = n.map(_.trim).filter(_.nonEmpty),
      variant = Variant.orDefault(v),
      clock = cl,
      days = days,
      rated = rated,
      position = pos,
      userIds = usernames.map(_.map(_.id)) collect { case List(w, b) =>
        (w, b)
      },
      rules = ~rules,
      expiresAt = expiresAt
    ).autoVariant
