package views.clas

import play.api.data.Form

import lila.ui.ContentSecurityPolicy
import lila.app.templating.Environment.{ *, given }
import lila.clas.{ Clas, Student }
import lila.rating.UserPerfsExt.{ bestRating, bestAny3Perfs }

lazy val ui = lila.clas.ui.ClasUi(helpers)
private lazy val dashUi = lila.clas.ui.DashboardUi(helpers, ui)(perfs =>
  _ ?=>
    td(dataSort := perfs.bestRating, cls := "rating")(
      perfs.bestAny3Perfs.map(showPerfRating(perfs, _))
    )
)

def studentDashboard(c: Clas, wall: Html, teachers: List[User], students: List[Student.WithUserPerfs])(using
    PageContext
) =
  layout(c.name, Left(c.withStudents(Nil)))(
    cls := "clas-show dashboard dashboard-student",
    dashUi.student(c, wall, teachers, students)
  )

object wall:

  def show(c: Clas, html: Html, students: List[Student.WithUser])(using PageContext) =
    teacherDashboard.layout(c, students.filter(_.student.isActive), "wall"):
      ui.wall.show(c, html)

  def edit(c: Clas, students: List[Student.WithUser], form: Form[?])(using PageContext) =
    teacherDashboard.layout(c, students, "wall"):
      ui.wall.edit(c, form)

private def layout(
    title: String,
    active: Either[Clas.WithStudents, String],
    student: Option[Student] = none,
    moreJs: Option[Frag] = none,
    csp: Option[ContentSecurityPolicy] = none
)(body: Modifier*)(using PageContext) =
  views.base.layout(
    title = title,
    moreCss = cssTag("clas"),
    modules = jsModule("bits.clas"),
    moreJs = moreJs,
    csp = csp
  )(
    if isGranted(_.Teacher) then
      main(cls := "page-menu")(
        ui.teacherMenu(active, student),
        div(cls := "page-menu__content box")(body)
      )
    else main(cls := "page-small box")(body)
  )
