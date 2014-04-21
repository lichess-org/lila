package lila.app
package templating

import play.api.templates.Html

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

  def aiNameHtml(level: Int, withRating: Boolean = true)(implicit ctx: UserContext) =
    Html(aiName(level, withRating).replace(" ", "&nbsp;"))

  def aiRating(level: Int): Option[Int] = Env.ai ratingOf level
}
