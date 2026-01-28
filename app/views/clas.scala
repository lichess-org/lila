package views.clas

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.clas.{ Clas, Student }
import lila.app.mashup.TeamInfo.PastAndNext

lazy val ui = lila.clas.ui.ClasUi(helpers)(views.mod.ui.menu("search"))
private lazy val dashUi = lila.clas.ui.DashboardUi(helpers, ui)

export dashUi.student.apply as studentDashboard
export dashUi.teacher as teacherDashboard

lazy val clas = lila.clas.ui.ClasPages(helpers, ui, dashUi)

object student:
  lazy val ui = lila.clas.ui.StudentUi(helpers, views.clas.ui)
  lazy val formUi = lila.clas.ui.StudentFormUi(helpers, views.clas.ui, ui)

  export ui.invite
  export formUi.{ newStudent as form, many as manyForm, edit, release, close, move }

  def show(
      clas: Clas,
      students: List[Student],
      s: Student.WithUserAndManagingClas,
      activities: Seq[lila.activity.ActivityView]
  )(using ctx: Context) =
    ui.show(clas, students, s, views.activity(s.withPerfs, activities))

def clasTournaments(tours: PastAndNext)(using Context) =
  tours.nonEmpty.option:
    st.section(cls := "clas-tournaments"):
      table(cls := "slist"):
        val allTours = tours.next ::: tours.past.take(5 - tours.next.size)
        val recentTours = allTours.filter(_.isRecent)
        views.team.tournaments.renderList(recentTours)
