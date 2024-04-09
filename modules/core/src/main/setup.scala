package lila.core
package setup

import lila.core.data.Days

trait OpenConfig:
  val name: Option[String]
  val variant: chess.variant.Variant
  val clock: Option[chess.Clock.Config]
  val days: Option[Days]
  val rated: Boolean
  val position: Option[chess.format.Fen.Full]
  val userIds: Option[PairOf[UserId]]
  val rules: Set[game.GameRule]
  val expiresAt: Option[Instant]

trait SetupForm:
  import play.api.data.Mapping
  private type Named[T] = (String, Mapping[T])
  def variant: Named[Option[chess.variant.Variant.LilaKey]]
  def message: Named[Option[String]]
  def clock: Named[Option[chess.Clock.Config]]
  def optionalDays: Named[Option[Days]]
  def rules: Named[Option[Set[game.GameRule]]]
