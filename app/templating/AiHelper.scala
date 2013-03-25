package lila.app
package templating

import lila.user.Context

trait AiHelper extends I18nHelper {

  val aiName: String = "Stockfish AI"

  def aiName(level: Int)(implicit ctx: Context): String = 
    trans.aiNameLevelAiLevel(aiName, level).body
}
