package views.html.clas

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }

object wall {

  def show(c: Clas, html: Frag, students: List[Student.WithUser])(implicit ctx: Context) =
    teacherDashboard.layout(c, students.filter(_.student.isActive), "wall")(
      div(cls := "clas-wall__actions")(
        a(dataIcon := "", href := routes.Clas.wallEdit(c.id.value), cls := "button button-clas text")(
          trans.clas.editNews()
        ),
        a(dataIcon := "", href := routes.Clas.notifyStudents(c.id.value), cls := "button button-clas text")(
          trans.clas.notifyAllStudents()
        )
      ),
      if (c.wall.isEmpty)
        div(cls := "box__pad clas-wall clas-wall--empty")(trans.clas.nothingHere())
      else
        div(cls := "box__pad clas-wall")(html)
    )

  def edit(c: Clas, students: List[Student.WithUser], form: Form[_])(implicit ctx: Context) =
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
        postForm(cls := "form3", action := routes.Clas.wallUpdate(c.id.value))(
          form3.globalError(form),
          form3.group(
            form("wall"),
            trans.clas.news()
          )(form3.textarea(_)(rows := 20)),
          form3.actions(
            a(href := routes.Clas.wall(c.id.value))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )
}
