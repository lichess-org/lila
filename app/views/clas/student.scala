package views.html.clas

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object student {

  def show(
      clas: Clas,
      students: List[Student],
      s: Student.WithUser,
      activities: Vector[lila.activity.ActivityView]
  )(implicit ctx: Context) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show",
      top(clas, s),
      div(cls := "box__pad")(
        standardFlash(),
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
        s.student.notes.nonEmpty option div(cls := "student-show__notes")(richText(s.student.notes)),
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

  private def top(clas: Clas, s: Student.WithUser) =
    div(cls := "student-show__top")(
      h1(dataIcon := "r")(
        span(
          strong(s.user.username),
          em(s.student.realName)
        )
      ),
      div(cls := "student-show__top__meta")(
        p(
          "Invited to ",
          a(href := routes.Clas.show(clas.id.value))(clas.name),
          " by ",
          userIdLink(s.student.created.by.value.some, withOnline = false),
          " ",
          momentFromNowOnce(s.student.created.at)
        ),
        div(
          a(
            href := routes.Clas.studentEdit(clas.id.value, s.user.username),
            cls := "button button-empty",
            title := "Edit student"
          )("Edit"),
          a(
            href := routes.User.show(s.user.username),
            cls := "button button-empty",
            title := "View full Lichess profile"
          )("Profile")
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
            th("Real name"),
            sortNumberTh("Rating"),
            sortNumberTh("Games"),
            sortNumberTh("Active")
          )
        ),
        tbody(
          students.sortBy(_.user.username).map {
            case Student.WithUser(student, user) =>
              tr(
                td(
                  if (teacher)
                    a(href := routes.Clas.studentShow(c.id.value, user.username))(
                      userSpan(user)
                    )
                  else userLink(user)
                ),
                td(student.realName),
                td(user.perfs.bestRating),
                td(user.count.game.localize),
                td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
              )
          }
        )
      )

  private def realNameField(form: Form[_])(implicit ctx: Context) =
    form3.group(
      form("realName"),
      frag("Real name"),
      help = frag("Private info, never visible on Lichess. Helps you remember who that student is.").some
    )(form3.input(_))

  def form(
      c: Clas,
      students: List[Student],
      invite: Form[_],
      create: Form[_],
      created: Option[lila.clas.Student.WithPassword] = none
  )(implicit ctx: Context) =
    bits.layout("Add student", Left(c withStudents students))(
      cls := "box-pad student-add",
      h1("Add student"),
      p(
        "To ",
        a(href := routes.Clas.show(c.id.value))(c.name)
      ),
      created map {
        case Student.WithPassword(student, password) =>
          flashMessage(cls := "student-add__created")(
            strong(
              "Lichess profile ",
              userIdLink(student.userId.some, withOnline = false),
              " created for ",
              student.realName,
              "."
            ),
            p(
              "Make sure to copy or write down the password now. You won’t be able to see it again!"
            ),
            code("Password: ", password.value)
          )
      },
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
          form3.group(invite("username"), frag("Lichess username"))(
            form3.input(_, klass = "user-autocomplete")(created.isEmpty option autofocus)(dataTag := "span")
          ),
          realNameField(invite),
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
          form3.group(create("username"), frag("Lichess username"))(
            form3.input(_)(created.isDefined option autofocus)
          ),
          realNameField(create),
          form3.submit(trans.signUp())
        )
      )
    )

  def edit(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[_])(implicit ctx: Context) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show student-edit",
      top(clas, s),
      div(cls := "box__pad")(
        standardFlash(),
        postForm(cls := "form3", action := routes.Clas.studentUpdate(clas.id.value, s.user.username))(
          form3.globalError(form),
          realNameField(form),
          form3.group(form("notes"), raw("Notes"), help = frag("Only visible to the class teachers").some)(
            form3.textarea(_)(autofocus, rows := 15)
          ),
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )
}
