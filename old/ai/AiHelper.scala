package lila.app
package ai

import http.Context
import i18n.I18nHelper

trait AiHelper extends I18nHelper {

  val aiName: String = "Stockfish AI"

  def aiName(level: Int)(implicit ctx: Context): String = 
    trans.aiNameLevelAiLevel(aiName, level).body
}
