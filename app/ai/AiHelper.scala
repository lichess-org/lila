package lila
package ai

import http.Context
import i18n.I18nHelper

trait AiHelper extends I18nHelper {

  def aiName = "Stockfish AI"

  def aiName(level: Int)(implicit ctx: Context) = 
    trans.aiNameLevelAiLevel("Crafty A.I.", level)
}
