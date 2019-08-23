package views.html.study

import lidraughts.app.templating.Environment._
import lidraughts.common.Lang
import lidraughts.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply()(implicit lang: Lang) =
    views.html.board.userAnalysisI18n(withAdvantageChart = true) ++
      i18nFullDbJsObject(lidraughts.i18n.I18nDb.Study) ++
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
