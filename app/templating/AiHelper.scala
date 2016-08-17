package lila.app
package templating

import play.twirl.api.Html

import chess.variant.Variant
import lila.user.UserContext

trait AiHelper { self: I18nHelper =>

  def aiName(variant: Variant): String =
    if (variant == chess.variant.Crazyhouse) "Sunsetter AI"
    else "Stockfish AI"

  def aiName(variant: Variant, level: Int, withRating: Boolean = true)(implicit ctx: UserContext): String = {
    val name = trans.aiNameLevelAiLevel.str(aiName(variant), level)
    val rating = withRating ?? {
      aiRating(level) ?? { r => s" ($r)" }
    }
    s"$name$rating"
  }

  def aiNameHtml(variant: Variant,level: Int, withRating: Boolean = true)(implicit ctx: UserContext) =
    Html(aiName(variant, level, withRating).replace(" ", "&nbsp;"))

  def aiRating(level: Int): Option[Int] = Env.fishnet.aiPerfApi.intRatings get level
}
