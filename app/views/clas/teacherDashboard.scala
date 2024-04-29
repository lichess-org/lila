package views.clas
package teacherDashboard

import lila.app.templating.Environment.{ *, given }

import lila.clas.{ Clas, ClasInvite, ClasProgress, Student }
import lila.common.String.html.richText
import lila.rating.PerfType
import lila.core.user.WithPerf
import lila.rating.PerfExt.showRatingProvisional
import lila.rating.UserPerfsExt.bestAny3Perfs
import lila.rating.UserPerfsExt.bestRating

private[clas] def layout(
    c: Clas,
    students: List[Student.WithUserLike],
    active: String
)(modifiers: Modifier*)(using PageContext) =
  views.clas.layout(c.name, Left(c.withStudents(students.map(_.student))))(
    cls := s"clas-show dashboard dashboard-teacher dashboard-teacher-$active",
    dashUi.teacher.layout(c, students, active),
    modifiers
  )

def overview(c: Clas, students: List[Student.WithUserPerfs])(using PageContext) =
  layout(c, students, "overview")(dashUi.teacher.overview(c, students))

def students(c: Clas, all: List[Student.WithUserPerfs], invites: List[ClasInvite])(using PageContext) =
  layout(c, all.filter(_.student.isActive), "students"):
    dashUi.teacher.students(c, all, invites)

def unreasonable(c: Clas, students: List[Student.WithUser], active: String)(using PageContext) =
  layout(c, students, active)(dashUi.teacher.unreasonable(students.size))

def progress(c: Clas, students: List[Student.WithUserPerf], progress: ClasProgress)(using PageContext) =
  layout(c, students, "progress")(dashUi.teacher.progress(c, students, progress))

def learn(
    c: Clas,
    students: List[Student.WithUser],
    basicCompletion: Map[UserId, Int],
    practiceCompletion: Map[UserId, Int],
    coordScores: Map[UserId, chess.ByColor[Int]]
)(using PageContext) =
  layout(c, students, "progress")(
    dashUi.teacher.learn(c, students, basicCompletion, practiceCompletion, coordScores)
  )
