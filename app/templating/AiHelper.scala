package lila.app
package templating

import lila.user.UserContext

trait AiHelper { self: I18nHelper =>

  val aiName: String = "Stockfish AI"

  def aiName(level: Int, withRating: Boolean = true)(implicit ctx: UserContext): String = {
    val name = trans.aiNameLevelAiLevel.str(aiName, level)
    val rating = withRating ?? {
      aiRating(level) ?? { r => s"&nbsp;($r)" }
    }
    s"$name$rating"
  }

  def aiRating(level: Int): Option[Int] = Env.ai ratingOf level
}
