package lila.app
package templating

import lila.user.UserContext

trait AiHelper { self: I18nHelper =>

  val aiName: String = "Stockfish AI"

  def aiName(level: Int)(implicit ctx: UserContext): String =
    trans.aiNameLevelAiLevel(aiName, level).body
}
