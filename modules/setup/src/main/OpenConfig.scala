package lila.setup

import chess.Clock
import chess.format.Fen
import chess.variant.{ FromPosition, Variant }
import scalalib.model.Days

import lila.core.game.GameRule

final case class OpenConfig(
    name: Option[String],
    variant: chess.variant.Variant,
    clock: Option[Clock.Config],
    days: Option[Days],
    rated: Boolean,
    position: Option[Fen.Full],
    userIds: Option[(UserId, UserId)],
    rules: Set[GameRule] = Set.empty,
    expiresAt: Option[Instant]
) extends lila.core.setup.OpenConfig:

  def perfType = lila.rating.PerfType(variant, chess.Speed(clock))

  def validFen = Variant.isValidInitialFen(variant, position)

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
      pos: Option[Fen.Full],
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
      userIds = usernames.map(_.map(_.id)).collect { case List(w, b) =>
        (w, b)
      },
      rules = ~rules,
      expiresAt = expiresAt
    ).autoVariant
