package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.pref.PrefCateg
import lila.user.User
import controllers.routes

object bits:

  def data(u: User)(using PageContext) =
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

  def categName(categ: PrefCateg)(using PageContext): String =
    categ match
      case PrefCateg.Display      => trans.preferences.display.txt()
      case PrefCateg.ChessClock   => trans.preferences.chessClock.txt()
      case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
      case PrefCateg.Privacy      => trans.preferences.privacy.txt()

  def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  def radios[A](field: play.api.data.Field, options: Iterable[(A, String)]) =
    st.group(cls := "radio")(
      options.map { (key, value) =>
        val id      = s"ir${field.id}_$key"
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

  def bitCheckboxes(field: play.api.data.Field, options: Iterable[(Int, String)]) =
    st.group(cls := "radio")(
      /// Will hold the value being calculated with the various checkboxes when sending
      div(
        input(
          st.id := s"ir${field.id}_hidden",
          true option st.checked,
          tpe      := "hidden",
          st.value := "",
          name     := field.name
        ),
        st.style := "display: none;"
      ) :: options
        .map: (key, value) =>
          val id      = s"ir${field.id}_$key"
          val intVal  = ~field.value.flatMap(_.toIntOption)
          val checked = (intVal & key) == key
          div(
            input(
              st.id := id,
              checked option st.checked,
              tpe               := "checkbox",
              st.value          := key.toString,
              attr("data-name") := field.name
            ),
            label(`for` := id)(value)
          )
        .toList
    )
