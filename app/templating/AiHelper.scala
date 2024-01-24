package lila.app
package templating

import play.api.i18n.Lang
import lila.game.EngineConfig

import lila.app.ui.ScalatagsTemplate._

trait AiHelper { self: I18nHelper =>

  def aiName(ec: EngineConfig)(implicit lang: Lang): String =
    trans.aiNameLevelAiLevel.txt(ec.engine.fullName, ec.level)

  def aiNameFrag(ec: EngineConfig)(implicit lang: Lang) =
    raw(aiName(ec).replace(" ", "&nbsp;"))

  def aiNameNoLang(ec: EngineConfig): String =
    s"${ec.engine.fullName} level ${ec.level}"
}
