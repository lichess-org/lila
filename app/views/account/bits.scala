package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.PrefCateg

object bits {

  def categName(categ: lila.pref.PrefCateg)(implicit ctx: Context): String =
    categ match {
      case PrefCateg.GameDisplay  => trans.preferences.gameDisplay.txt()
      case PrefCateg.ShogiClock   => trans.preferences.shogiClock.txt()
      case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
      case PrefCateg.Privacy      => trans.privacy.txt()
    }
}
