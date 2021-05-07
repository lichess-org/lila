package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.PrefCateg
import lila.user.User
import controllers.routes

object bits {

  def data(u: User)(implicit ctx: Context) =
    account.layout(title = s"${u.username} - personal data", active = "security") {
      div(cls := "account security personal-data box box-pad")(
        h1("My personal data"),
        div(cls := "personal-data__header")(
          p("Here is all personal information Lichess has about ", userLink(u)),
          a(cls := "button", href := s"${routes.Account.data}?user=${u.id}&text=1", downloadAttr)(
            trans.downloadRaw()
          )
        )
      )
    }

  def categName(categ: lila.pref.PrefCateg)(implicit ctx: Context): String =
    categ match {
      case PrefCateg.GameDisplay  => trans.preferences.gameDisplay.txt()
      case PrefCateg.ChessClock   => trans.preferences.chessClock.txt()
      case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
      case PrefCateg.Privacy      => trans.privacy.txt()
    }
}
