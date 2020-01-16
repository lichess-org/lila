package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student, Teacher }
import controllers.routes

object student {

  def show(
      clas: Clas,
      student: Student.WithUser,
      password: Option[lila.user.User.ClearPassword] = none
  )(implicit ctx: Context) =
    views.html.clas.clas.layout(student.user.username, "student")(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(student.user.username),
        p(
          a(href := routes.Clas.show(clas.id.value))(clas.name)
        ),
        div(cls := "box__top__actions")(
          a(
            href := routes.User.show(student.user.username),
            cls := "button button-empty"
          )("User profile")
        )
      ),
      password map { pass =>
        div(cls := "box__pad password")(
          iconTag("E")(cls := "is-green"),
          div(
            p(
              "Make sure to copy or write down the password now.",
              br,
              "You won’t be able to see it again!"
            ),
            code(pass.value),
            div(
              a(
                href := routes.Clas.studentCreate(clas.id.value),
                cls := "button button-green text",
                dataIcon := "O"
              )("Add another student")
            )
          )
        )
      }
    )

  def list(students: List[Student.WithUser])(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th("Student"),
            th("Games"),
            th("Active")
          )
        ),
        tbody(
          students.map {
            case Student.WithUser(_, user) =>
              tr(
                td(userLink(user, withBestRating = true)),
                td(user.count.game.localize),
                td(user.seenAt.map(momentFromNow(_)))
              )
          }
        )
      )

  def form(c: lila.clas.Clas, form: Form[String])(implicit ctx: Context) =
    clas.layout("Add student", "newStudent")(
      cls := "box-pad",
      h1("Add student"),
      p(
        "To ",
        a(href := routes.Clas.show(c.id.value))(c.name)
      ),
      postForm(cls := "form3", action := routes.Clas.studentCreate(c.id.value))(
        form3.group(form("username"), frag("Username"))(form3.input(_)(autofocus)),
        form3.actions(
          a(href := routes.Clas.show(c.id.value))(trans.cancel()),
          form3.submit(trans.signUp())
        )
      )
    )
}
