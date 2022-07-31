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
            trans.download()
          )
        )
      )
    }

  def categName(categ: lila.pref.PrefCateg)(implicit ctx: Context): String =
    categ match {
      case PrefCateg.Display      => trans.preferences.display.txt()
      case PrefCateg.ChessClock   => trans.preferences.chessClock.txt()
      case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
      case PrefCateg.Privacy      => trans.preferences.privacy.txt()
      case PrefCateg.Notification => trans.preferences.notifications.txt()
    }

  def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  def radios(field: play.api.data.Field, options: Iterable[(Any, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { v =>
        val id      = s"${field.id}_${v._1}"
        val checked = field.value has v._1.toString
        div(
          input(
            st.id := s"$prefix$id",
            checked option st.checked,
            tpe   := "radio",
            value := v._1.toString,
            name  := field.name
          ),
          label(`for` := s"$prefix$id")(v._2)
        )
      }.toList
    )
}
