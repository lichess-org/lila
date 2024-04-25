package views.html.clas

import lila.web.ContentSecurityPolicy
import lila.app.templating.Environment.{ *, given }

import lila.clas.{ Clas, Student }

lazy val ui = lila.clas.ui.ClasUi(helpers)

object bits:

  def layout(
      title: String,
      active: Either[Clas.WithStudents, String],
      student: Option[Student] = none,
      moreJs: Option[Frag] = none,
      csp: Option[ContentSecurityPolicy] = none
  )(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas"),
      modules = jsModule("bits.clas"),
      moreJs = moreJs,
      csp = csp
    )(
      if isGranted(_.Teacher) then
        main(cls := "page-menu")(
          lila.ui.bits.pageMenuSubnav(
            a(cls := active.toOption.map(_.active("classes")), href := routes.Clas.index)(
              trans.clas.lichessClasses()
            ),
            active.left.toOption.map { clas =>
              frag(
                a(cls := "active", href := routes.Clas.show(clas.clas.id.value))(clas.clas.name),
                clas.students.map { s =>
                  a(
                    cls  := List("student" -> true, "active" -> student.exists(s.is)),
                    href := routes.Clas.studentShow(clas.clas.id.value, s.userId)
                  )(
                    titleNameOrId(s.userId),
                    em(s.realName)
                  )
                }
              )
            } | {
              a(cls := active.toOption.map(_.active("newClass")), href := routes.Clas.form)(
                trans.clas.newClass()
              )
            }
          ),
          div(cls := "page-menu__content box")(body)
        )
      else main(cls := "page-small box")(body)
    )
