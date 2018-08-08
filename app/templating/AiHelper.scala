package lidraughts.app
package templating

import play.twirl.api.Html

import lidraughts.user.UserContext

trait AiHelper { self: I18nHelper =>

  def aiName(level: Int, withRating: Boolean = true)(implicit ctx: UserContext): String = {
    val name = lidraughts.i18n.I18nKeys.aiNameLevelAiLevel.txt("Stockfish AI", level)
    val rating = withRating ?? {
      aiRating(level) ?? { r => s" ($r)" }
    }
    s"$name$rating"
  }

  def aiNameHtml(level: Int, withRating: Boolean = true)(implicit ctx: UserContext) =
    Html(aiName(level, withRating).replace(" ", "&nbsp;"))

  def aiRating(level: Int): Option[Int] = None //Env.fishnet.aiPerfApi.intRatings get level
}
