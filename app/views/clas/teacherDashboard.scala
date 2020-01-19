package views.html.clas

import controllers.routes
import lila.api.Context
import lila.rating.PerfType
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object teacherDashboard {

  private def dashboard(
      c: Clas,
      students: List[Student.WithUser],
      active: String
  )(content: Frag)(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents students.map(_.student)))(
      cls := "clas-show dashboard dashboard-teacher",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(c.name),
        div(cls := "box__top__actions")(
          a(
            href := routes.Clas.edit(c.id.value),
            cls := "button button-empty"
          )("Edit"),
          a(
            href := routes.Clas.studentForm(c.id.value),
            cls := "button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      div(cls := "box__pad")(
        standardFlash(),
        c.desc.nonEmpty option div(cls := "clas-desc")(richText(c.desc)),
        c.archived map { archived =>
          div(cls := "clas-show__archived archived")(
            bits.showArchived(archived),
            postForm(action := routes.Clas.archive(c.id.value, false))(
              form3.submit("Restore", icon = none)(
                cls := "confirm button-empty",
                title := "Revive the class"
              )
            )
          )
        },
        clas.teachers(c)
      ),
      st.nav(cls := "dashboard-nav tabs-horiz")(
        a(cls := active.active("students"), href := routes.Clas.show(c.id.value))("Students"),
        List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)
          .map { pt =>
            a(cls := active.active(pt.key), href := routes.Clas.perfType(c.id.value, pt.key))(pt.name),
          },
        a(cls := active.active("archived"), href := routes.Clas.archived(c.id.value))("Archived")
      ),
      content
    )

  def active(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    dashboard(c, students, "students")(
      studentList(c, students)
    )

  def archived(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    dashboard(c, students.filter(_.student.isActive), "archived")(
      studentList(c, students.filter(_.student.isArchived))
    )

  def perf(
      c: Clas,
      students: List[Student.WithUser],
      perfType: PerfType
  )(implicit ctx: Context) =
    dashboard(c, students, perfType.key)(
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(attr("data-sort-default") := "1")(),
              sortNumberTh("Rating"),
              sortNumberTh("Games"),
              sortNumberTh("Progress")
            )
          ),
          tbody(
            students.sortBy(_.user.username).map {
              case s @ Student.WithUser(_, user) =>
                tr(
                  studentTd(c, s),
                  td(dataSort := user.perfs(perfType).intRating, cls := "rating")(
                    user.perfs(perfType).showRatingProvisional
                  ),
                  td(user.perfs(perfType).nb),
                  td(dataSort := user.perfs(perfType).progress)(user.perfs(perfType).progress)
                )
            }
          )
        )
      )
    )

  private def studentList(c: Clas, students: List[Student.WithUser])(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(attr("data-sort-default") := "1")("Student"),
              th("Real name"),
              sortNumberTh("Rating"),
              sortNumberTh("Games"),
              sortNumberTh("Puzzles"),
              sortNumberTh("Active")
            )
          ),
          tbody(
            students.sortBy(_.user.username).map {
              case s @ Student.WithUser(student, user) =>
                tr(
                  studentTd(c, s),
                  td(student.realName),
                  td(dataSort := user.perfs.bestRating, cls := "rating")(user.best3Perfs.map {
                    showPerfRating(user, _)
                  }),
                  td(user.count.game.localize),
                  td(user.perfs.puzzle.nb),
                  td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
                )
            }
          )
        )
      )

  private def studentTd(c: Clas, s: Student.WithUser) =
    td(
      a(href := routes.Clas.studentShow(c.id.value, s.user.username))(
        userSpan(
          s.user,
          name = span(
            strong(s.user.username),
            em(s.student.realName)
          ).some
        )
      )
    )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")
}
