package views.html.study

import lila.app.templating.Environment._
import lila.common.Lang
import lila.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply()(implicit lang: Lang) =
    views.html.board.userAnalysisI18n(withAdvantageChart = true) ++
      i18nFullDbJsObject(lila.i18n.I18nDb.Study) ++
      i18nJsObject(translations)

  private val translations = Vector(
    trans.name,
    trans.white,
    trans.black,
    trans.variant,
    trans.clearBoard,
    trans.startPosition,
    trans.cancel,
    trans.chat
  )
}
