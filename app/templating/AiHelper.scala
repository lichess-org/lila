package lila.app
package templating

import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._

trait AiHelper { self: I18nHelper =>

  def aiName(level: Int)(implicit lang: Lang): String =
    trans.aiNameLevelAiLevel.txt("Stockfish", level)

  def aiNameFrag(level: Int)(implicit lang: Lang) =
    raw(aiName(level).replace(" ", "&nbsp;"))
}
