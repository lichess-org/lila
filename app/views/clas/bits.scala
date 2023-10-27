package views.html.clas

import controllers.clas.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, Student }

object bits:

  def layout(
      title: String,
      active: Either[Clas.WithStudents, String],
      student: Option[Student] = none
  )(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas"),
      moreJs = jsModule("clas")
    )(
      if isGranted(_.Teacher) then
        main(cls := "page-menu")(
          views.html.site.bits.pageMenuSubnav(
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

  def showArchived(archived: Clas.Recorded)(using PageContext) =
    div(
      trans.clas.removedByX(userIdLink(archived.by.some)),
      " ",
      momentFromNowOnce(archived.at)
    )
