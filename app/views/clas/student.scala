package views.clas

import play.api.data.Form
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }

import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object student:

  lazy val ui     = lila.clas.ui.StudentUi(helpers, views.clas.ui)
  lazy val formUi = lila.clas.ui.StudentFormUi(helpers, ui)

  def show(
      clas: Clas,
      students: List[Student],
      s: Student.WithUserAndManagingClas,
      activities: Vector[lila.activity.ActivityView]
  )(using ctx: PageContext) =
    layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show",
      ui.show(clas, students, s, views.activity(s.withPerfs, activities))
    )

  def form(
      clas: Clas,
      students: List[Student],
      invite: Form[?],
      create: Form[?],
      nbStudents: Int,
      created: Option[lila.clas.Student.WithPassword] = none
  )(using PageContext) =
    layout(trans.clas.addStudent.txt(), Left(clas.withStudents(students)))(
      cls := "box-pad student-add",
      formUi.newStudent(clas, students, invite, create, nbStudents, created)
    )

  def manyForm(
      clas: Clas,
      students: List[Student],
      form: Form[?],
      nbStudents: Int,
      created: Seq[lila.clas.Student.WithPassword] = Nil
  )(using PageContext) =
    layout(trans.clas.addStudent.txt(), Left(clas.withStudents(students)))(
      cls := "box-pad student-add-many",
      formUi.many(clas, students, form, nbStudents, created)
    )

  def edit(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using PageContext) =
    layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit",
      formUi.edit(clas, students, s, form)
    )

  def release(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using PageContext) =
    layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit",
      ui.top(clas, s),
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
            trans.site.email(),
            help = trans.clas.realUniqueEmail().some
          )(form3.input(_, typ = "email")(autofocus, required)),
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id.value, s.user.username))(trans.site.cancel()),
            form3.submit(trans.site.apply())
          )
        )
      )
    )

  def close(clas: Clas, students: List[Student], s: Student.WithUser)(using PageContext) =
    layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
      cls := "student-show student-edit",
      ui.top(clas, s),
      div(cls := "box__pad")(
        h2(trans.clas.closeTheAccount()),
        p(strong(badTag(trans.clas.closeDesc1()))),
        p(
          a(href := routes.Clas.studentRelease(clas.id.value, s.user.username))(trans.clas.closeDesc2())
        ),
        postForm(cls := "form3", action := routes.Clas.studentClosePost(clas.id.value, s.user.username))(
          form3.actions(
            a(href := routes.Clas.studentShow(clas.id.value, s.user.username))(trans.site.cancel()),
            form3.submit(trans.clas.closeTheAccount(), icon = Icon.CautionCircle.some)(
              cls := "button-red confirm"
            )
          )
        )
      )
    )
