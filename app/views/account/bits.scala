package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.PrefCateg

object bits {

  def categName(categ: lila.pref.PrefCateg)(implicit ctx: Context) = categ match {
    case PrefCateg.GameDisplay => trans.gameDisplay()
    case PrefCateg.ChessClock => trans.chessClock()
    case PrefCateg.GameBehavior => trans.gameBehavior()
    case PrefCateg.Privacy => trans.privacy()
  }
}
