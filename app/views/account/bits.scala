package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.pref.PrefCateg

object bits {

  def categName(categ: lidraughts.pref.PrefCateg)(implicit ctx: Context) = categ match {
    case PrefCateg.GameDisplay => trans.gameDisplay()
    case PrefCateg.DraughtsClock => trans.draughtsClock()
    case PrefCateg.GameBehavior => trans.gameBehavior()
    case PrefCateg.Privacy => trans.privacy()
  }
}
