package views.html
package account

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.pref.PrefCateg
import lila.user.User
import controllers.routes

object bits:

  def data(u: User)(implicit ctx: Context) =
    account.layout(title = s"${u.username} - personal data", active = "security") {
      div(cls := "account security personal-data box box-pad")(
        h1(cls := "box__top")("My personal data"),
        div(cls := "personal-data__header")(
          p("Here is all personal information Lichess has about ", userLink(u)),
          a(cls := "button", href := s"${routes.Account.data}?user=${u.id}&text=1", downloadAttr)(
            trans.download()
          )
        )
      )
    }

  def categName(categ: lila.pref.PrefCateg)(implicit ctx: Context): String =
    categ match
      case PrefCateg.Display      => trans.preferences.display.txt()
      case PrefCateg.ChessClock   => trans.preferences.chessClock.txt()
      case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
      case PrefCateg.Privacy      => trans.preferences.privacy.txt()

  def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  def radios[A](field: play.api.data.Field, options: Iterable[(A, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { (key, value) =>
        val id      = s"$prefix${field.id}_$key"
        val checked = field.value has key.toString
        div(
          input(
            st.id := id,
            checked option st.checked,
            tpe      := "radio",
            st.value := key.toString,
            name     := field.name
          ),
          label(`for` := id)(value)
        )
      }.toList
    )
