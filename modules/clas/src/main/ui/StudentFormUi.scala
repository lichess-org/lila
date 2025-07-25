package lila.clas
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudentFormUi(helpers: Helpers, clasUi: ClasUi, studentUi: StudentUi):
  import helpers.{ *, given }
  import clasUi.ClasPage

  private def maxStudentsWarning(using Translate) =
    p(dataIcon := Icon.InfoCircle, cls := "text")(
      trans.clas.maxStudentsNote(
        Clas.maxStudents,
        a(href := routes.Clas.form)(trans.clas.createMoreClasses())
      )
    )

  private def realNameField(form: Form[?], fieldName: String = "realName")(using Context) =
    form3.group(
      form(fieldName),
      trans.clas.realName(),
      help = trans.clas.privateWillNeverBeShown().some
    )(form3.input(_))

  def newStudent(
      clas: Clas,
      students: List[Student],
      invite: Form[?],
      create: Form[?],
      nbStudents: Int,
      created: Option[Student.WithPassword] = none
  )(using Context) =
    ClasPage(trans.clas.addStudent.txt(), Left(clas.withStudents(students)))(cls := "box-pad student-add"):
      frag(
        boxTop(
          h1(
            trans.clas.addStudent(),
            s" ($nbStudents/${Clas.maxStudents})"
          )
        ),
        (nbStudents > (Clas.maxStudents / 2)).option(maxStudentsWarning),
        created.map { case Student.WithPassword(student, password) =>
          flashMessageWith(cls := "flash flash-quiet student-add__created")(
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
        (nbStudents < Clas.maxStudents).option(
          frag(
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
              postForm(cls := "form3", action := routes.Clas.studentInvite(clas.id))(
                form3.group(invite("username"), trans.clas.lichessUsername())(field =>
                  div(cls := "complete-parent")(
                    form3.input(field, klass = "user-autocomplete")(created.isEmpty.option(autofocus))(
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
              postForm(cls := "form3", action := routes.Clas.studentCreate(clas.id))(
                form3.group(
                  create("create-username"),
                  trans.clas.lichessUsername(),
                  help = a(cls := "name-regen", href := s"${routes.Clas.studentForm(clas.id)}?gen=1")(
                    trans.clas.generateANewUsername()
                  ).some
                )(
                  form3.input(_)(created.isDefined.option(autofocus))
                ),
                realNameField(create, "create-realName"),
                form3.submit(trans.site.signUp(), icon = none)
              )
            ),
            div(cls := "student-add__or")(trans.clas.orSeparator()),
            div(cls := "student-add__choice")(
              div(cls := "info")(
                h2(trans.clas.createMultipleAccounts()),
                trans.clas.multipleAccsFormDescription(
                  a(href := routes.Clas.studentManyForm(clas.id))(
                    trans.clas.useThisForm()
                  )
                )
              )
            )
          )
        )
      )

  def many(
      clas: Clas,
      students: List[Student],
      form: Form[?],
      nbStudents: Int,
      created: Seq[Student.WithPassword] = Nil
  )(using Context) =
    ClasPage(trans.clas.addStudent.txt(), Left(clas.withStudents(students)))(
      cls := "box-pad student-add-many"
    ):
      frag(
        h1(cls := "box__top")(trans.clas.createMultipleAccounts()),
        maxStudentsWarning,
        created.nonEmpty.option(
          frag(
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
                    th(trans.site.password())
                  )
                ),
                tbody(
                  created.map { case Student.WithPassword(student, password) =>
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
          )
        ),
        (nbStudents < Clas.maxStudents).option(
          frag(
            p(badTag(strong(trans.clas.createStudentWarning()))),
            postForm(cls := "form3", action := routes.Clas.studentManyCreate(clas.id))(
              form3.globalError(form),
              form3.group(
                form("realNames"),
                trans.clas.studentsRealNamesOnePerLine(),
                help = trans.clas.privateWillNeverBeShown().some
              )(
                form3.textarea(_)(autofocus, rows := 20)
              ),
              form3.submit(trans.site.apply(), icon = none)
            )
          )
        )
      )

  def edit(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using Context) =
    ClasPage(s.user.username.value, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit"
    ):
      frag(
        studentUi.top(clas, s),
        div(cls := "box__pad")(
          standardFlash,
          postForm(cls := "form3", action := routes.Clas.studentUpdate(clas.id, s.user.username))(
            form3.globalError(form),
            realNameField(form),
            form3.group(form("notes"), trans.site.notes(), help = trans.clas.onlyVisibleToTeachers().some)(
              form3.textarea(_)(autofocus, rows := 15)
            ),
            form3.actions(
              a(href := routes.Clas.studentShow(clas.id, s.user.username))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          ),
          hr,
          div(cls := "student-show__other-actions")(
            s.student.isActive.option(
              postForm(
                action := routes.Clas.studentArchive(clas.id, s.user.username, v = true)
              )(
                form3.submit(trans.clas.removeStudent(), icon = none)(
                  cls := "yes-no-confirm button-red button-empty"
                )
              )
            ),
            s.student.managed.option(
              a(
                href := routes.Clas.studentClose(clas.id, s.user.username),
                cls := "button button-empty button-red",
                title := trans.clas.closeDesc1.txt()
              )(trans.clas.closeStudent())
            ),
            a(
              href := routes.Clas.studentMove(clas.id, s.user.username),
              cls := "button button-empty"
            )(trans.clas.moveToAnotherClass())
          )
        )
      )

  def release(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using Context) =
    ClasPage(s.user.username.value, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit"
    ):
      frag(
        studentUi.top(clas, s),
        div(cls := "box__pad")(
          h2(trans.clas.releaseTheAccount()),
          p(
            trans.clas.releaseDesc1(),
            br,
            trans.clas.releaseDesc2()
          ),
          postForm(cls := "form3", action := routes.Clas.studentReleasePost(clas.id, s.user.username))(
            form3.globalError(form),
            form3.group(
              form("email"),
              trans.site.email(),
              help = trans.clas.realUniqueEmail().some
            )(form3.input(_, typ = "email")(autofocus, required)),
            form3.actions(
              a(href := routes.Clas.studentShow(clas.id, s.user.username))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          )
        )
      )

  def move(clas: Clas, students: List[Student], s: Student.WithUser, otherClasses: List[Clas])(using
      Context
  ) =
    ClasPage(s.user.username.value, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit"
    ):

      val classForms: Frag = otherClasses.map: toClass =>
        postForm(action := routes.Clas.studentMovePost(clas.id, s.student.userId, toClass.id))(
          form3.submit(toClass.name, icon = Icon.InternalArrow.some)(
            cls := "yes-no-confirm button-blue button-empty",
            title := trans.clas.moveToClass.txt(toClass.name)
          )
        )

      frag(
        studentUi.top(clas, s),
        div(cls := "box__pad")(
          h2(trans.clas.moveToAnotherClass()),
          classForms,
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id, s.user.username))(trans.site.cancel())
          )
        )
      )

  def close(clas: Clas, students: List[Student], s: Student.WithUser)(using Context) =
    ClasPage(s.user.username.value, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit"
    ):
      frag(
        studentUi.top(clas, s),
        div(cls := "box__pad")(
          h2(trans.clas.closeTheAccount()),
          p(strong(badTag(trans.clas.closeDesc1()))),
          p(
            a(href := routes.Clas.studentRelease(clas.id, s.user.username))(trans.clas.closeDesc2())
          ),
          postForm(cls := "form3", action := routes.Clas.studentClosePost(clas.id, s.user.username))(
            form3.actions(
              a(href := routes.Clas.studentShow(clas.id, s.user.username))(trans.site.cancel()),
              form3.submit(trans.clas.closeTheAccount(), icon = Icon.CautionCircle.some)(
                cls := "button-red yes-no-confirm"
              )
            )
          )
        )
      )
