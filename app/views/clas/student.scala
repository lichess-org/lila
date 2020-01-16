package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import controllers.routes

object student {

  def show(
      clas: Clas,
      student: Student.WithUser,
      password: Option[lila.user.User.ClearPassword] = none
  )(implicit ctx: Context) =
    bits.layout(student.user.username, Left(clas))(
      cls := "student-show",
      div(cls := "box__top")(
        div(cls := "student-show__title", dataIcon := "r")(
          div(
            h1(student.user.username),
            a(href := routes.Clas.show(clas.id.value))(clas.name)
          )
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
              "Make sure to copy or write down the password now. You won’t be able to see it again!"
            ),
            code(s"Password: ${pass.value}"),
            a(
              href := routes.Clas.studentCreate(clas.id.value),
              cls := "button button-green text",
              dataIcon := "O"
            )("Add another student")
          )
        )
      }
    )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")

  def list(c: Clas, students: List[Student.WithUser])(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      table(cls := "slist slist-pad sortable")(
        thead(
          tr(
            th(attr("data-sort-default") := "1")("Student"),
            sortNumberTh("Rating"),
            sortNumberTh("Games"),
            sortNumberTh("Active")
          )
        ),
        tbody(
          students.sortBy(_.user.username).map {
            case Student.WithUser(_, user) =>
              tr(
                td(a(href := routes.Clas.studentShow(c.id.value, user.username))(user.username)),
                td(user.perfs.bestRating),
                td(user.count.game.localize),
                td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
              )
          }
        )
      )

  def form(c: lila.clas.Clas, form: Form[String])(implicit ctx: Context) =
    bits.layout("Add student", Left(c))(
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
