package views.html.clas

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }

object wall {

  def show(
      c: Clas,
      html: Frag,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    teacherDashboard.layout(c, students.filter(_.student.isActive), "wall")(
      div(cls := "clas-center")(
        a(href := routes.Clas.wallEdit(c.id.value), cls := "button button-clas")("Edit news")
      ),
      if (c.wall.isEmpty)
        div(cls := "box__pad clas-wall clas-wall--empty")("Nothing here, yet.")
      else
        div(cls := "box__pad clas-wall")(html)
    )

  def edit(
      c: Clas,
      students: List[Student.WithUser],
      form: Form[_]
  )(implicit ctx: Context) =
    teacherDashboard.layout(c, students, "wall")(
      div(cls := "box-pad")(
        postForm(cls := "form3", action := routes.Clas.wallUpdate(c.id.value))(
          form3.globalError(form),
          form3.group(
            form("wall"),
            frag("Class news"),
            help = frag("Add the most recent news at the top.").some
          )(form3.textarea(_)(rows := 20)),
          form3.actions(
            a(href := routes.Clas.wall(c.id.value))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )
}
