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
        ctx.flash("password").map { password =>
          flashMessage(cls := "student-show__password")(
            div(
              p(trans.clas.makeSureToCopy()),
              pre(trans.clas.passwordX(password))
            )
          )
        },
        s.student.archived map { archived =>
          div(cls := "student-show__archived archived")(
            bits.showArchived(archived),
            postForm(action := routes.Clas.studentArchive(clas.id.value, s.user.username, false))(
              form3.submit(trans.clas.inviteTheStudentBack(), icon = none)(cls := "confirm button-empty")
            )
          )
        },
        s.student.notes.nonEmpty option div(cls := "student-show__notes")(richText(s.student.notes)),
        s.student.managed option div(cls := "student-show__managed")(
          p(trans.clas.thisStudentAccountIsManaged()),
          div(cls := "student-show__managed__actions")(
            postForm(action := routes.Clas.studentSetKid(clas.id.value, s.user.username, !s.user.kid))(
              form3.submit(if (s.user.kid) trans.disableKidMode() else trans.enableKidMode(), icon = none)(
                s.student.isArchived option disabled,
                cls   := List("confirm button button-empty" -> true, "disabled" -> s.student.isArchived),
                title := trans.kidModeExplanation.txt()
              )
            ),
            postForm(action := routes.Clas.studentResetPassword(clas.id.value, s.user.username))(
              form3.submit(trans.clas.resetPassword(), icon = none)(
                s.student.isArchived option disabled,
                cls   := List("confirm button button-empty" -> true, "disabled" -> s.student.isArchived),
                title := trans.clas.generateANewPassword.txt()
              )
            ),
            a(
              href  := routes.Clas.studentRelease(clas.id.value, s.user.username),
              cls   := "button button-empty",
              title := trans.clas.upgradeFromManaged.txt()
            )(trans.clas.release())
          )
        ),
        views.html.activity(s.user, activities)
      )
    )

  private def top(clas: Clas, s: Student.WithUser)(implicit ctx: Context) =
    div(cls := "student-show__top")(
      h1(dataIcon := "r")(
        span(
          strong(s.user.username),
          em(s.student.realName)
        )
      ),
      div(cls := "student-show__top__meta")(
        p(
          trans.clas.invitedToXByY(
            a(href := routes.Clas.show(clas.id.value))(clas.name),
            userIdLink(s.student.created.by.some, withOnline = false)
          ),
          " ",
          momentFromNowOnce(s.student.created.at)
        ),
        div(
          a(
            href := routes.Msg.convo(s.user.username),
            cls  := "button button-empty"
          )(trans.message()),
          a(
            href := routes.Clas.studentEdit(clas.id.value, s.user.username),
            cls  := "button button-empty"
          )(trans.edit()),
          a(
            href := routes.User.show(s.user.username),
            cls  := "button button-empty"
          )(trans.profile())
        )
      )
    )

  private def realNameField(form: Form[_], fieldName: String = "realName")(implicit ctx: Context) =
    form3.group(
      form(fieldName),
      trans.clas.realName(),
      help = trans.clas.privateWillNeverBeShown().some
    )(form3.input(_))

  def form(
      c: Clas,
      students: List[Student],
      invite: Form[_],
      create: Form[_],
      nbStudents: Int,
      created: Option[lila.clas.Student.WithPassword] = none
  )(implicit ctx: Context) =
    bits.layout(trans.clas.addStudent.txt(), Left(c withStudents students))(
      cls := "box-pad student-add",
      h1(
        trans.clas.addStudent(),
        s" ($nbStudents/${lila.clas.Clas.maxStudents})"
      ),
      nbStudents > (lila.clas.Clas.maxStudents / 2) option p(dataIcon := "", cls := "text")(
        s"Note that a class can have up to ${lila.clas.Clas.maxStudents} students.",
        "To manage more students, ",
        a(href := routes.Clas.studentForm(c.id.value))("create more classes"),
        "."
      ),
      created map { case Student.WithPassword(student, password) =>
        flashMessage(cls := "student-add__created")(
          strong(
            trans.clas.lishogiProfileXCreatedForY(
              userIdLink(student.userId.some, withOnline = false),
              student.realName
            ),
            p(trans.clas.makeSureToCopy()),
            pre(
              trans.clas.studentCredentials(student.realName, usernameOrId(student.userId), password.value)
            )
          )
        )
      },
      standardFlash(),
      (nbStudents <= lila.clas.Clas.maxStudents) option frag(
        div(cls := "student-add__choice")(
          div(cls := "info")(
            h2(trans.clas.inviteALishogiAccount()),
            p(trans.clas.inviteDesc1()),
            p(trans.clas.inviteDesc2()),
            p(
              strong(trans.clas.inviteDesc3()),
              br,
              trans.clas.inviteDesc4()
            )
          ),
          postForm(cls := "form3", action := routes.Clas.studentInvite(c.id.value))(
            form3.group(invite("username"), trans.clas.lishogiUsername())(
              form3.input(_, klass = "user-autocomplete")(created.isEmpty option autofocus)(dataTag := "span")
            ),
            realNameField(invite),
            form3.submit("Invite", icon = none)
          )
        ),
        div(cls := "student-add__or")("~ or ~"),
        div(cls := "student-add__choice")(
          div(cls := "info")(
            h2(trans.clas.createANewLishogiAccount()),
            p(trans.clas.createDesc1()),
            p(trans.clas.createDesc2()),
            p(strong(trans.clas.createDesc3()), br, trans.clas.createDesc4())
          ),
          postForm(cls := "form3", action := routes.Clas.studentCreate(c.id.value))(
            form3.group(
              create("create-username"),
              trans.clas.lishogiUsername(),
              help = a(cls := "name-regen", href := s"${routes.Clas.studentForm(c.id.value)}?gen=1")(
                trans.clas.generateANewUsername()
              ).some
            )(
              form3.input(_)(created.isDefined option autofocus)
            ),
            realNameField(create, "create-realName"),
            form3.submit(trans.signUp(), icon = none)
          )
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
          form3.group(form("notes"), trans.notes(), help = trans.clas.onlyVisibleToTeachers().some)(
            form3.textarea(_)(autofocus, rows := 15)
          ),
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.apply())
          )
        ),
        s.student.isActive option frag(
          hr,
          postForm(
            action := routes.Clas.studentArchive(clas.id.value, s.user.username, true),
            cls    := "student-show__archive"
          )(
            form3.submit(trans.clas.removeStudent(), icon = none)(
              cls := "confirm button-red button-empty"
            )
          )
        )
      )
    )

  def release(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[_])(implicit
      ctx: Context
  ) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show student-edit",
      top(clas, s),
      div(cls := "box__pad")(
        h2(trans.clas.releaseTheAccount()),
        p(
          trans.clas.releaseDesc1(),
          br,
          trans.clas.releaseDesc2()
        ),
        postForm(cls := "form3", action := routes.Clas.studentReleasePost(clas.id.value, s.user.username))(
          form3.globalError(form),
          form3.group(
            form("email"),
            trans.email(),
            help = trans.clas.realUniqueEmail().some
          )(form3.input(_, typ = "email")(autofocus, required)),
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )
}
