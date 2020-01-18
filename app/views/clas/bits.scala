package views.html.clas

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import controllers.routes

object bits {

  def layout(
      title: String,
      active: Either[Clas.WithStudents, String],
      student: Option[Student] = none
  )(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas"),
      moreJs = jsAt("compiled/clas.js")
    )(
      if (isGranted(_.Teacher))
        main(cls := "page-menu")(
          st.nav(cls := "page-menu__menu subnav")(
            a(cls := active.toOption.map(_.active("classes")), href := routes.Clas.index)("Lichess Classes"),
            active.left.toOption.map { clas =>
              frag(
                a(cls := "active", href := routes.Clas.show(clas.clas.id.value))(clas.clas.name),
                clas.students.map { s =>
                  a(
                    cls := List("student" -> true, "active" -> student.exists(s.is)),
                    href := routes.Clas.studentShow(clas.clas.id.value, s.userId)
                  )(
                    usernameOrId(s.userId),
                    em(s.realName)
                  )
                }
              )
            } | {
              a(cls := active.toOption.map(_.active("newClass")), href := routes.Clas.form)("New class")
            }
          ),
          div(cls := "page-menu__content box")(body)
        )
      else main(cls := "page-small box")(body)
    )

  def showArchived(archived: Clas.Recorded) =
    div(
      "Archived by ",
      userIdLink(archived.by.value.some),
      " ",
      momentFromNowOnce(archived.at)
    )
}
