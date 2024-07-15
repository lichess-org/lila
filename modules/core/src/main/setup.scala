package lila.core
package setup

import _root_.chess.variant.Variant
import _root_.chess.{ Clock, format }
import scalalib.model.Days

import lila.core.game.GameRule
import lila.core.userId.UserId

trait OpenConfig:
  val name: Option[String]
  val variant: Variant
  val clock: Option[Clock.Config]
  val days: Option[Days]
  val rated: Boolean
  val position: Option[format.Fen.Full]
  val userIds: Option[PairOf[UserId]]
  val rules: Set[game.GameRule]
  val expiresAt: Option[Instant]

trait SetupForm:
  import play.api.data.Mapping
  private type Named[T] = (String, Mapping[T])
  def variant: Named[Option[Variant.LilaKey]]
  def message: Named[Option[String]]
  def clock: Named[Option[Clock.Config]]
  def optionalDays: Named[Option[Days]]
  def rules: Named[Option[Set[GameRule]]]
