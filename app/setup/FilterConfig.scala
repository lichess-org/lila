package lila
package setup

import chess.{ Variant, Mode, Color ⇒ ChessColor }
import game.{ DbGame, DbPlayer }

case class FilterConfig(variant: Option[Variant]) {

  def encode = RawFilterConfig(
    v = ~variant.map(_.id)
  )
}

object FilterConfig {

  val default = FilterConfig(
    variant = none)

  val variants = 0 :: Config.variants
  val variantChoices = variants map { id ⇒
    (Config.variantChoices.toMap get id.toString map ("Only " +)) | "All"
  }
}

private[setup] case class RawFilterConfig(v: Int) {

  def decode = FilterConfig(
    variant = Variant(v) 
  ).some
}
