package lila.app
package templating

import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate.*

trait AiHelper:
  self: I18nHelper =>

  def aiName(level: Int)(using Lang): String =
    trans.aiNameLevelAiLevel.txt("Stockfish", level)

  def aiNameFrag(level: Int)(using Lang) =
    raw(aiName(level).replace(" ", "&nbsp;"))
