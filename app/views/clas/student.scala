package views.html.clas

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }

object student {

  def show(
      clas: Clas,
      s: Student.WithUser,
      activities: Vector[lila.activity.ActivityView]
  )(implicit ctx: Context) =
    bits.layout(s.user.username, Left(clas))(
      cls := "student-show",
      div(cls := "box__top")(
        div(cls := "student-show__title", dataIcon := "r")(
          div(
            h1(s.user.username),
            p(
              "Invited to ",
              a(href := routes.Clas.show(clas.id.value))(clas.name),
              " by ",
              userIdLink(s.student.created.by.value.some),
              " ",
              momentFromNowOnce(s.student.created.at)
            )
          )
        ),
        div(cls := "box__top__actions")(
          a(
            href := routes.User.show(s.user.username),
            cls := "button button-empty",
            title := "View full Lichess profile"
          )("profile")
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
            s.student.isVeryNew option a(
              href := routes.Clas.studentForm(clas.id.value),
              cls := "button button-green text",
              dataIcon := "O"
            )("Add another student")
          )
        )
      },
      div(cls := "box__pad")(
        standardFlash(),
        div(
          ),
        s.student.archived map { archived =>
          div(cls := "student-show__archived")(
            div(
              "Archived by ",
              userIdLink(archived.by.value.some),
              " ",
              momentFromNowOnce(archived.at)
            ),
            postForm(action := routes.Clas.studentArchive(clas.id.value, s.user.username, false))(
              form3.submit("Restore", icon = none)(
                cls := "confirm button-empty",
                title := "Get the student back into the class"
              )
            )
          )
        },
        s.student.managed option div(cls := "student-show__managed")(
          p("This student account is managed"),
          div(cls := "student-show__managed__actions")(
            postForm(action := routes.Clas.studentSetKid(clas.id.value, s.user.username, !s.user.kid))(
              form3.submit(if (s.user.kid) "Disable kid mode" else "Enable kid mode", icon = none)(
                s.student.isArchived option disabled,
                cls := List("confirm button-empty" -> true, "disabled" -> s.student.isArchived),
                title := "Kid mode prevents the student from communicating with Lichess players"
              )
            ),
            postForm(action := routes.Clas.studentResetPassword(clas.id.value, s.user.username))(
              form3.submit("Reset password", icon = none)(
                s.student.isArchived option disabled,
                cls := List("confirm button-empty" -> true, "disabled" -> s.student.isArchived),
                title := "Generate a new password for the student"
              )
            )
          )
        ),
        views.html.activity(s.user, activities),
        s.student.isActive option postForm(
          action := routes.Clas.studentArchive(clas.id.value, s.user.username, true),
          cls := "student-show__archive"
        )(
          form3.submit("Archive", icon = none)(
            cls := "confirm button-red button-empty",
            title := "Remove the student from the class"
          )
        )
      )
    )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")

  def list(c: Clas, students: List[Student.WithUser], teacher: Boolean)(title: Frag)(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      table(cls := s"slist slist-pad ${teacher ?? " sortable"}")(
        thead(
          tr(
            th(attr("data-sort-default") := "1")(title),
            sortNumberTh("Rating"),
            sortNumberTh("Games"),
            sortNumberTh("Active")
          )
        ),
        tbody(
          students.sortBy(_.user.username).map {
            case Student.WithUser(_, user) =>
              tr(
                td(
                  if (teacher)
                    a(href := routes.Clas.studentShow(c.id.value, user.username))(
                      userSpan(user)
                    )
                  else userLink(user)
                ),
                td(user.perfs.bestRating),
                td(user.count.game.localize),
                td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
              )
          }
        )
      )

  private def realName(form: Form[_])(implicit ctx: Context) =
    form3.group(
      form("realName"),
      frag("Real name"),
      help = frag("Private info, never visible on Lichess. Helps you remember who that student is.").some
    )(form3.input(_))

  def form(c: lila.clas.Clas, invite: Form[_], create: Form[_])(implicit ctx: Context) =
    bits.layout("Add student", Left(c))(
      cls := "box-pad student-add",
      h1("Add student"),
      p(
        "To ",
        a(href := routes.Clas.show(c.id.value))(c.name)
      ),
      standardFlash(),
      div(cls := "student-add__choice")(
        div(cls := "info")(
          h2("Invite a Lichess account"),
          p("If the student already has a Lichess account, you can invite them to the class."),
          p("They will receive a message on Lichess with a link to join the class."),
          p(
            strong("Important: only invite students you know, and who actively want to join the class."),
            br,
            "Never send unsolicited invites to arbitrary players."
          )
        ),
        postForm(cls := "form3", action := routes.Clas.studentInvite(c.id.value))(
          form3.group(invite("invite"), frag("Invite username"))(
            form3.input(_, klass = "user-autocomplete")(autofocus)(dataTag := "span")
          ),
          realName(invite),
          form3.submit("Invite")
        )
      ),
      div(cls := "student-add__or")("~ or ~"),
      div(cls := "student-add__choice")(
        div(cls := "info")(
          h2("Create a new Lichess account"),
          p("If the student doesn't have a Lichess account yet, you can create one for them here."),
          p(
            "No email address is required. A password will be generated, ",
            "and you will have to transmit it to the student, so they can log in."
          ),
          p(
            strong("Important: a student must not have multiple accounts."),
            br,
            "If they already have one, use the invite form instead."
          )
        ),
        postForm(cls := "form3", action := routes.Clas.studentCreate(c.id.value))(
          form3.group(create("username"), frag("Create username"))(form3.input(_)(autofocus)),
          realName(create),
          form3.submit(trans.signUp())
        )
      )
    )
}
