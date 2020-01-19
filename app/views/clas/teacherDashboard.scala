package views.html.clas

import controllers.routes
import lila.api.Context
import lila.rating.PerfType
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, ClasProgress, Student }
import lila.common.String.html.richText

object teacherDashboard {

  private def dashboard(
      c: Clas,
      students: List[Student.WithUser],
      active: String
  )(modifiers: Modifier*)(implicit ctx: Context) =
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
        }
      ),
      st.nav(cls := "dashboard-nav tabs-horiz")(
        a(cls := active.active("overview"), href := routes.Clas.show(c.id.value))("Overview"),
        a(cls := active.active("progress"), href := routes.Clas.progress(c.id.value, PerfType.Blitz.key, 7))(
          "Progress"
        ),
        a(cls := active.active("archived"), href := routes.Clas.archived(c.id.value))("Archived")
      ),
      modifiers
    )

  def overview(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    dashboard(c, students, "overview")(
      div(cls := "clas-show__overview")(
        c.desc.trim.nonEmpty option div(cls := "clas-desc")(richText(c.desc)),
        clas.teachers(c)
      ),
      if (students.isEmpty)
        p(cls := "box__pad students__empty")("No students in the class, yet.")
      else
        studentList(c, students)
    )

  def archived(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    dashboard(c, students.filter(_.student.isActive), "archived") {
      val archived = students.filter(_.student.isArchived)
      if (archived.isEmpty)
        p(cls := "box__pad students__empty")("No archived students.")
      else
        studentList(c, archived)
    }

  def progress(
      c: Clas,
      students: List[Student.WithUser],
      progress: ClasProgress
  )(implicit ctx: Context) =
    dashboard(c, students, "progress")(
      div(cls := "progress")(
        div(cls := "progress-perf")(
          label("Variant"),
          div(cls := "progress-choices")(
            List(
              PerfType.Bullet,
              PerfType.Blitz,
              PerfType.Rapid,
              PerfType.Classical,
              PerfType.Correspondence,
              PerfType.Puzzle
            ).map { pt =>
              a(
                cls := progress.perfType.key.active(pt.key),
                href := routes.Clas.progress(c.id.value, pt.key, progress.days)
              )(pt.name),
            }
          )
        ),
        div(cls := "progress-days")(
          label("Over days"),
          div(cls := "progress-choices")(
            List(1, 2, 3, 7, 10, 14, 21, 30, 60, 90).map { days =>
              a(
                cls := progress.days.toString.active(days.toString),
                href := routes.Clas.progress(c.id.value, progress.perfType.key, days)
              )(days)
            }
          )
        )
      ),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(attr("data-sort-default") := "1")(
                s"${progress.perfType.name} over last ${progress.days} days"
              ),
              sortNumberTh("Rating"),
              sortNumberTh("Progress"),
              sortNumberTh(if (progress.isPuzzle) "Puzzles" else "Games"),
              if (progress.isPuzzle) sortNumberTh("Winrate")
              else sortNumberTh("Time playing")
            )
          ),
          tbody(
            students.sortBy(_.user.username).map {
              case s @ Student.WithUser(_, user) =>
                val prog = progress(user)
                tr(
                  studentTd(c, s),
                  td(dataSort := user.perfs(progress.perfType).intRating, cls := "rating")(
                    user.perfs(progress.perfType).showRatingProvisional
                  ),
                  td(dataSort := prog.ratingProgress)(ratingProgress(prog.ratingProgress) | "N/A"),
                  td(prog.nb),
                  if (progress.isPuzzle) td(dataSort := prog.winRate)(prog.winRate, "%")
                  else td(dataSort := prog.millis)(showPeriod(prog.period))
                )
            }
          )
        )
      )
    )

  private def studentList(c: Clas, students: List[Student.WithUser])(implicit ctx: Context) =
    div(cls := "students")(
      table(cls := "slist slist-pad sortable")(
        thead(
          tr(
            th(attr("data-sort-default") := "1")("Student"),
            sortNumberTh("Rating"),
            sortNumberTh("Games"),
            sortNumberTh("Puzzles"),
            sortNumberTh("Active"),
            th(iconTag("5")(title := "Managed"))
          )
        ),
        tbody(
          students.sortBy(_.user.username).map {
            case s @ Student.WithUser(student, user) =>
              tr(
                studentTd(c, s),
                td(dataSort := user.perfs.bestRating, cls := "rating")(user.best3Perfs.map {
                  showPerfRating(user, _)
                }),
                td(user.count.game.localize),
                td(user.perfs.puzzle.nb),
                td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce)),
                td(student.managed option iconTag("5")(title := "Managed"))
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
            s.user.username,
            em(s.student.realName)
          ).some,
          withTitle = false
        )
      )
    )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")
}
