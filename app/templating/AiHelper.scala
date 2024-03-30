package lila.app
package templating

import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate.*
import lila.core.i18n.Translate

trait AiHelper:
  self: I18nHelper =>

  def aiName(level: Int)(using Translate): String =
    trans.site.aiNameLevelAiLevel.txt("Stockfish", level)

  def aiNameFrag(level: Int)(using Translate) =
    raw(aiName(level).replace(" ", "&nbsp;"))
