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
      student: Student.WithUser
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
      ctx.flash("password") map { pass =>
        div(cls := "box__pad password")(
          iconTag("E")(cls := "is-green"),
          div(
            p(
              "Make sure to copy or write down the password now. You won’t be able to see it again!"
            ),
            code(s"Password: $pass"),
            a(
              href := routes.Clas.studentForm(clas.id.value),
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

  def form(c: lila.clas.Clas, invite: Form[String], create: Form[String])(implicit ctx: Context) =
    bits.layout("Add student", Left(c))(
      cls := "box-pad student-add",
      h1("Add student"),
      p(
        "To ",
        a(href := routes.Clas.show(c.id.value))(c.name)
      ),
      ctx.flash("success") map { msg =>
        div(cls := "flash-success")(msg)
      },
      div(cls := "student-add__choice")(
        div(cls := "student-add__choice__invite")(
          h2("Invite a Lichess account"),
          p(
            "If the student already has a Lichess account, ",
            "you can invite them to the class. ",
            "They will receive a message on Lichess with a link to join the class.",
            strong("Important: only invite students you know, and who actively want to join the class."),
            "Never send unsolicited invites to arbitrary players."
          ),
          postForm(cls := "form3", action := routes.Clas.studentInvite(c.id.value))(
            form3.group(invite("invite"), frag("Invite username"))(
              form3.input(_, klass = "user-autocomplete")(autofocus)(dataTag := "span")
            ),
            form3.submit("Invite")
          )
        ),
        div(cls := "student-add__choice__create")(
          h2("Create a new Lichess account"),
          p(
            "If the student doesn't have a Lichess account yet, ",
            "you can create one for them here. ",
            br,
            "No email address is required. A password will be generated, ",
            "and you will have to transmit it to the student, so they can log in.",
            br,
            strong("Important: a student must not have multiple accounts."),
            " ",
            "If they already have one, use the invite form instead."
          ),
          postForm(cls := "form3", action := routes.Clas.studentCreate(c.id.value))(
            form3.group(create("username"), frag("Create username"))(form3.input(_)(autofocus)),
            form3.submit(trans.signUp())
          )
        )
      )
    )
}
