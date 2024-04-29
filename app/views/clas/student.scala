package views.clas
package student

import play.api.data.Form
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }

import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

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

def invite(c: Clas, invite: lila.clas.ClasInvite)(using PageContext) =
  views.base.layout(moreCss = cssTag("clas"), title = c.name)(ui.invite(c, invite))

def release(clas: Clas, students: List[Student], s: Student.WithUser, form: Form[?])(using PageContext) =
  layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
    cls := "student-show student-edit",
    formUi.release(clas, students, s, form)
  )

def close(clas: Clas, students: List[Student], s: Student.WithUser)(using PageContext) =
  layout(s.user.username, Left(clas.withStudents(students)), s.student.some)(
    cls := "student-show student-edit",
    formUi.close(clas, students, s)
  )
