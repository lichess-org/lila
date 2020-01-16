package views.html.clas

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student, Teacher }
import controllers.routes

object bits {

  def layout(title: String, active: Either[Clas, String])(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas"),
      moreJs = jsAt("compiled/clas.js")
    )(
      main(cls := "page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          a(cls := active.toOption.map(_.active("classes")), href := routes.Clas.index)("Lichess Classes"),
          active.left.toOption.map { clas =>
            a(cls := "active", href := routes.Clas.show(clas.id.value))(clas.name)
          } | {
            a(cls := active.toOption.map(_.active("newClass")), href := routes.Clas.form)("New class")
          }
        ),
        div(cls := "page-menu__content box")(body)
      )
    )
}
