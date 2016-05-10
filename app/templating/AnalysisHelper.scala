package lila.app
package templating

import lila.analyse.Advice.Judgment
import play.api.data._

import lila.api.Context

trait AnalysisHelper { self: I18nHelper with SecurityHelper =>

  def judgmentName(judgment: Judgment)(implicit ctx: Context) = judgment match {
    case Judgment.Blunder    => trans.blunders()
    case Judgment.Mistake    => trans.mistakes()
    case Judgment.Inaccuracy => trans.inaccuracies()
    case judgment            => judgment.toString
  }
}
