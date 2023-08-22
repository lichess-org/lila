package views.html.clas

import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.routes
import play.api.data.Form
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object student:

  def show(
      clas: Clas,
      students: List[Student],
      s: Student.WithUserAndManagingClas,
      activities: Vector[lila.activity.ActivityView]
  )(using ctx: PageContext) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show",
      top(clas, s.withUser),
      div(cls := "box__pad")(
        standardFlash,
        ctx.flash("password").map { password =>
          flashMessageWith(cls := "student-show__password")(
            div(
              p(trans.clas.makeSureToCopy()),
              pre(trans.clas.passwordX(password))
            )
          )
        },
        s.student.archived map { archived =>
          div(cls := "student-show__archived archived")(
            bits.showArchived(archived),
            div(cls := "student-show__archived__actions")(
              postForm(action := clasRoutes.studentArchive(clas.id.value, s.user.username, v = false)):
                form3.submit(trans.clas.inviteTheStudentBack(), icon = none)(cls := "confirm button-empty")
              ,
              postForm(action := clasRoutes.studentClosePost(clas.id.value, s.user.username)):
                form3.submit(trans.clas.removeStudent(), icon = none)(
                  cls   := "confirm button-red button-empty",
                  title := "Fully erase the student from the class archives."
                )
            )
          )
        },
        s.student.notes.nonEmpty option div(cls := "student-show__notes")(richText(s.student.notes)),
        s.student.managed option div(cls := "student-show__managed")(
          p(trans.clas.thisStudentAccountIsManaged()),
          div(cls := "student-show__managed__actions")(
            postForm(action := clasRoutes.studentResetPassword(clas.id.value, s.user.username))(
              form3.submit(trans.clas.resetPassword(), icon = none)(
                s.student.isArchived option disabled,
                cls   := List("confirm button button-empty" -> true, "disabled" -> s.student.isArchived),
                title := trans.clas.generateANewPassword.txt()
              )
            ),
            a(
              href  := clasRoutes.studentRelease(clas.id.value, s.user.username),
              cls   := "button button-empty",
              title := trans.clas.upgradeFromManaged.txt()
            )(trans.clas.release())
          )
        ) orElse s.managingClas.map { managingClas =>
          div(cls := "student-show__managed")(
            p(trans.clas.thisStudentAccountIsManaged()),
            a(href := clasRoutes.studentShow(managingClas.id.value, s.user.username))(
              "Class: ",
              managingClas.name
            )
          )
        },
        views.html.activity(s.withPerfs, activities)
      )
    )

  private def top(clas: Clas, s: Student.WithUserLike)(using Context) =
    div(cls := "student-show__top")(
      boxTop(
        h1(dataIcon := licon.User)(
          span(
            strong(s.user.username),
            em(s.student.realName)
          )
        )
      ),
      div(cls := "student-show__top__meta")(
        p(
          trans.clas.invitedToXByY(
            a(href := clasRoutes.show(clas.id.value))(clas.name),
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
            href := clasRoutes.studentEdit(clas.id.value, s.user.username),
            cls  := "button button-empty"
          )(trans.edit()),
          a(
            href := routes.User.show(s.user.username),
            cls  := "button button-empty"
          )(trans.profile()),
          a(
            href := routes.Puzzle.dashboard(7, "home", s.user.username.value.some),
            cls  := "button button-empty"
          )(trans.puzzle.puzzleDashboard()),
          isGranted(_.Beta) option a(
            href := routes.Tutor.user(s.user.username.value),
            cls  := "button button-empty"
          )("Tutor")
        )
      )
    )

  private def realNameField(form: Form[?], fieldName: String = "realName")(using Context) =
    form3.group(
      form(fieldName),
      trans.clas.realName(),
      help = trans.clas.privateWillNeverBeShown().some
    )(form3.input(_))

  def form(
      clas: Clas,
      students: List[Student],
      invite: Form[?],
      create: Form[?],
      nbStudents: Int,
      created: Option[lila.clas.Student.WithPassword] = none
  )(using PageContext) =
    bits.layout(trans.clas.addStudent.txt(), Left(clas withStudents students))(
      cls := "box-pad student-add",
      boxTop(
        h1(
          trans.clas.addStudent(),
          s" ($nbStudents/${lila.clas.Clas.maxStudents})"
        )
      ),
      nbStudents > (lila.clas.Clas.maxStudents / 2) option maxStudentsWarning,
      created map { case Student.WithPassword(student, password) =>
        flashMessageWith(cls := "student-add__created")(
          strong(
            trans.clas.lichessProfileXCreatedForY(
              userIdLink(student.userId.some, withOnline = false),
              student.realName
            ),
            p(trans.clas.makeSureToCopy()),
            pre(
              trans.clas.studentCredentials(student.realName, titleNameOrId(student.userId), password.value)
            )
          )
        )
      },
      standardFlash,
      (nbStudents < lila.clas.Clas.maxStudents) option frag(
        div(cls := "student-add__choice")(
          div(cls := "info")(
            h2(trans.clas.inviteALichessAccount()),
            p(trans.clas.inviteDesc1()),
            p(trans.clas.inviteDesc2()),
            p(
              strong(trans.clas.inviteDesc3()),
              br,
              trans.clas.inviteDesc4()
            )
          ),
          postForm(cls := "form3", action := clasRoutes.studentInvite(clas.id.value))(
            form3.group(invite("username"), trans.clas.lichessUsername())(field =>
              div(cls := "complete-parent")(
                form3.input(field, klass = "user-autocomplete")(created.isEmpty option autofocus)(
                  dataTag := "span"
                )
              )
            ),
            realNameField(invite),
            form3.submit(trans.clas.invite(), icon = none)
          )
        ),
        div(cls := "student-add__or")(trans.clas.orSeparator()),
        div(cls := "student-add__choice")(
          div(cls := "info")(
            h2(trans.clas.createANewLichessAccount()),
            p(trans.clas.createDesc1()),
            p(trans.clas.createDesc2()),
            p(strong(trans.clas.createDesc3()), br, trans.clas.createDesc4()),
            badTag(strong(trans.clas.createStudentWarning()))
          ),
          postForm(cls := "form3", action := clasRoutes.studentCreate(clas.id.value))(
            form3.group(
              create("create-username"),
              trans.clas.lichessUsername(),
              help = a(cls := "name-regen", href := s"${clasRoutes.studentForm(clas.id.value)}?gen=1")(
                trans.clas.generateANewUsername()
              ).some
            )(
              form3.input(_)(created.isDefined option autofocus)
            ),
            realNameField(create, "create-realName"),
            form3.submit(trans.signUp(), icon = none)
          )
        ),
        div(cls := "student-add__or")(trans.clas.orSeparator()),
        div(cls := "student-add__choice")(
          div(cls := "info")(
            h2(trans.clas.createMultipleAccounts()),
            trans.clas.multipleAccsFormDescription(
              a(href := clasRoutes.studentManyForm(clas.id.value))(
                trans.clas.useThisForm()
              )
            )
          )
        )
      )
    )

  def manyForm(
      clas: Clas,
      students: List[Student],
      form: Form[?],
      nbStudents: Int,
      created: Seq[lila.clas.Student.WithPassword] = Nil
  )(using PageContext) =
    bits.layout(trans.clas.addStudent.txt(), Left(clas withStudents students))(
      cls := "box-pad student-add-many",
      h1(cls := "box__top")(trans.clas.createMultipleAccounts()),
      maxStudentsWarning,
      created.nonEmpty option frag(
        flashMessageWith(cls := "student-add-many__created")(
          s"${created.size} students accounts have been created."
        ),
        div(cls := "student-add-many__list")(
          p(strong(trans.clas.makeSureToCopy())),
          table(cls := "slist")(
            thead(
              tr(
                th(trans.clas.realName()),
                th(trans.clas.lichessUsername()),
                th(trans.password())
              )
            ),
            tbody(
              created map { case Student.WithPassword(student, password) =>
                tr(
                  td(student.realName),
                  td(titleNameOrId(student.userId)),
                  td(password.value)
                )
              }
            )
          )
        ),
        br
      ),
      (nbStudents < lila.clas.Clas.maxStudents) option frag(
        p(badTag(strong(trans.clas.createStudentWarning()))),
        postForm(cls := "form3", action := clasRoutes.studentManyCreate(clas.id.value))(
          form3.globalError(form),
          form3.group(
            form("realNames"),
            trans.clas.studentsRealNamesOnePerLine(),
            help = trans.clas.privateWillNeverBeShown().some
          )(
            form3.textarea(_)(autofocus, rows := 20)
          ),
          form3.submit(trans.apply(), icon = none)
        )
      )
    )

  private def maxStudentsWarning(using Lang) =
    p(dataIcon := licon.InfoCircle, cls := "text")(
      trans.clas.maxStudentsNote(
        lila.clas.Clas.maxStudents,
        a(href := clasRoutes.form)(trans.clas.createMoreClasses())
      )
    )

  def edit(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using PageContext) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show student-edit",
      top(clas, s),
      div(cls := "box__pad")(
        standardFlash,
        postForm(cls := "form3", action := clasRoutes.studentUpdate(clas.id.value, s.user.username))(
          form3.globalError(form),
          realNameField(form),
          form3.group(form("notes"), trans.notes(), help = trans.clas.onlyVisibleToTeachers().some)(
            form3.textarea(_)(autofocus, rows := 15)
          ),
          form3.actions(
            a(href := clasRoutes.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.apply())
          )
        ),
        hr,
        div(cls := "student-show__other-actions")(
          s.student.isActive option
            postForm(
              action := clasRoutes.studentArchive(clas.id.value, s.user.username, v = true)
            )(
              form3.submit(trans.clas.removeStudent(), icon = none)(
                cls := "confirm button-red button-empty"
              )
            ),
          s.student.managed option a(
            href  := clasRoutes.studentClose(clas.id.value, s.user.username),
            cls   := "button button-empty button-red",
            title := trans.clas.closeDesc1.txt()
          )(trans.clas.closeStudent())
        )
      )
    )

  def release(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using PageContext) =
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
        postForm(cls := "form3", action := clasRoutes.studentReleasePost(clas.id.value, s.user.username))(
          form3.globalError(form),
          form3.group(
            form("email"),
            trans.email(),
            help = trans.clas.realUniqueEmail().some
          )(form3.input(_, typ = "email")(autofocus, required)),
          form3.actions(
            a(href := clasRoutes.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.apply())
          )
        )
      )
    )

  def close(clas: Clas, students: List[Student], s: Student.WithUser)(using PageContext) =
    bits.layout(s.user.username, Left(clas withStudents students), s.student.some)(
      cls := "student-show student-edit",
      top(clas, s),
      div(cls := "box__pad")(
        h2(trans.clas.closeTheAccount()),
        p(strong(badTag(trans.clas.closeDesc1()))),
        p(
          a(href := clasRoutes.studentRelease(clas.id.value, s.user.username))(trans.clas.closeDesc2())
        ),
        postForm(cls := "form3", action := clasRoutes.studentClosePost(clas.id.value, s.user.username))(
          form3.actions(
            a(href := clasRoutes.studentShow(clas.id.value, s.user.username))(trans.cancel()),
            form3.submit(trans.clas.closeTheAccount(), icon = licon.CautionCircle.some)(
              cls := "button-red confirm"
            )
          )
        )
      )
    )
