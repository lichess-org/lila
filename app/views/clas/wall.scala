package views.html.clas

import controllers.clas.routes.{ Clas as clasRoutes }
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, Student }

object wall:

  def show(c: Clas, html: Html, students: List[Student.WithUser])(using PageContext) =
    teacherDashboard.layout(c, students.filter(_.student.isActive), "wall")(
      div(cls := "clas-wall__actions")(
        a(
          dataIcon := licon.Pencil,
          href     := clasRoutes.wallEdit(c.id.value),
          cls      := "button button-clas text"
        )(
          trans.clas.editNews()
        ),
        a(
          dataIcon := licon.Envelope,
          href     := clasRoutes.notifyStudents(c.id.value),
          cls      := "button button-clas text"
        )(
          trans.clas.notifyAllStudents()
        )
      ),
      if c.wall.value.isEmpty then div(cls := "box__pad clas-wall clas-wall--empty")(trans.clas.nothingHere())
      else div(cls := "box__pad clas-wall")(rawHtml(html))
    )

  def edit(c: Clas, students: List[Student.WithUser], form: Form[?])(using PageContext) =
    teacherDashboard.layout(c, students, "wall")(
      div(cls := "box-pad clas-wall__edit")(
        p(
          strong(trans.clas.newsEdit1()),
          ul(
            li(trans.clas.newsEdit2()),
            li(trans.clas.newsEdit3()),
            li(markdownAvailable)
          )
        ),
        postForm(cls := "form3", action := clasRoutes.wallUpdate(c.id.value))(
          form3.globalError(form),
          form3.group(
            form("wall"),
            trans.clas.classNews()
          )(form3.textarea(_)(rows := 20)),
          form3.actions(
            a(href := clasRoutes.wall(c.id.value))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )
