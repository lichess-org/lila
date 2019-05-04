package lila.app
package templating

import lila.app.ui.ScalatagsTemplate._
import lila.user.UserContext

trait AiHelper { self: I18nHelper =>

  def aiName(level: Int, withRating: Boolean = true)(implicit ctx: UserContext): String = {
    val name = lila.i18n.I18nKeys.aiNameLevelAiLevel.txt("Stockfish AI", level)
    val rating = withRating ?? {
      aiRating(level) ?? { r => s" ($r)" }
    }
    s"$name$rating"
  }

  def aiNameFrag(level: Int, withRating: Boolean = true)(implicit ctx: UserContext) =
    raw(aiName(level, withRating).replace(" ", "&nbsp;"))

  def aiRating(level: Int): Option[Int] = Env.fishnet.aiPerfApi.intRatings get level
}
