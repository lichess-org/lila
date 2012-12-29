package lila
package setup

import chess.{ Variant, Mode, Color ⇒ ChessColor }
import game.{ DbGame, DbPlayer }

case class FilterConfig(variant: Option[Variant]) {

  def encode = RawFilterConfig(
    v = ~variant.map(_.id)
  )

  def >> = Some((variant map (_.id)))

  def render = Map("variant" -> variant.map(_.toString))
}

object FilterConfig {

  val default = FilterConfig(
    variant = none)

  val variants = 0 :: Config.variants

  def <<(v: Option[Int]) = new FilterConfig(
    variant = v flatMap Variant.apply
  )
}

private[setup] case class RawFilterConfig(v: Int) {

  def decode = FilterConfig(
    variant = Variant(v) 
  ).some
}
