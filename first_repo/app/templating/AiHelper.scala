package lila.app
package templating

import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._

trait AiHelper { self: I18nHelper =>

  def aiName(level: Int, withRating: Boolean = true)(implicit lang: Lang): String = {
    val name = trans.aiNameLevelAiLevel.txt("Stockfish", level)
    val rating = withRating ?? {
      aiRating(level) ?? { r =>
        s" ($r)"
      }
    }
    s"$name$rating"
  }

  def aiNameFrag(level: Int, withRating: Boolean = true)(implicit lang: Lang) =
    raw(aiName(level, withRating).replace(" ", "&nbsp;"))

  def aiRating(level: Int): Option[Int] = env.fishnet.aiPerfApi.intRatings get level
}
