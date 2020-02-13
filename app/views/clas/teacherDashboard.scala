package views.html.clas

import controllers.routes
import lila.api.Context
import lila.rating.PerfType
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, ClasProgress, Student }
import lila.common.String.html.richText

object teacherDashboard {

  import bits.{ dataSort, sortNumberTh }

  private[clas] def layout(
      c: Clas,
      students: List[Student.WithUser],
      active: String
  )(modifiers: Modifier*)(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents students.map(_.student)))(
      cls := s"clas-show dashboard dashboard-teacher dashboard-teacher-$active",
      div(cls := "clas-show__top")(
        h1(dataIcon := "f", cls := "text")(c.name),
        st.nav(cls := "dashboard-nav")(
          a(cls := active.active("overview"), href := routes.Clas.show(c.id.value))("Overview"),
          a(cls := active.active("wall"), href := routes.Clas.wall(c.id.value))("News"),
          a(
            cls := active.active("progress"),
            href := routes.Clas.progress(c.id.value, PerfType.Blitz.key, 7)
          )(trans.clas.progress()),
          a(cls := active.active("edit"), href := routes.Clas.edit(c.id.value))(trans.edit()),
          a(cls := active.active("archived"), href := routes.Clas.archived(c.id.value))(
            trans.clas.removedStudents()
          )
        )
      ),
      standardFlash(),
      c.archived map { archived =>
        div(cls := "clas-show__archived archived")(
          bits.showArchived(archived),
          postForm(action := routes.Clas.archive(c.id.value, false))(
            form3.submit(trans.clas.reopen(), icon = none)(cls := "confirm button-empty")
          )
        )
      },
      modifiers
    )

  def overview(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    layout(c, students, "overview")(
      div(cls := "clas-show__overview")(
        c.desc.trim.nonEmpty option div(cls := "clas-show__desc")(richText(c.desc)),
        div(cls := "clas-show__overview__manage")(
          clas.teachers(c),
          a(
            href := routes.Clas.studentForm(c.id.value),
            cls := "button button-clas text",
            dataIcon := "O"
          )(trans.clas.addStudent())
        )
      ),
      if (students.isEmpty)
        p(cls := "box__pad students__empty")(trans.clas.noStudents())
      else
        studentList(c, students)
    )

  def archived(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    layout(c, students.filter(_.student.isActive), "archived") {
      val archived = students.filter(_.student.isArchived)
      if (archived.isEmpty)
        p(cls := "box__pad students__empty")(trans.clas.noRemovedStudents())
      else
        studentList(c, archived)
    }

  def progress(
      c: Clas,
      students: List[Student.WithUser],
      progress: ClasProgress
  )(implicit ctx: Context) =
    layout(c, students, "progress")(
      div(cls := "progress")(
        div(cls := "progress-perf")(
          label(trans.variant()),
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
              )(pt.trans),
            }
          )
        ),
        div(cls := "progress-days")(
          label(trans.clas.overDays()),
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
                trans.clas.variantXOverLastY(progress.perfType.trans, trans.nbDays.txt(progress.days)),
                sortNumberTh(trans.rating()),
                sortNumberTh(trans.clas.progress()),
                sortNumberTh(if (progress.isPuzzle) trans.puzzles() else trans.games()),
                if (progress.isPuzzle) sortNumberTh(trans.clas.winrate())
                else sortNumberTh(trans.clas.timePlaying())
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
                    td(dataSort := prog.ratingProgress)(
                      ratingProgress(prog.ratingProgress) | trans.clas.na.txt()
                    ),
                    td(prog.nb),
                    if (progress.isPuzzle) td(dataSort := prog.winRate)(prog.winRate, "%")
                    else td(dataSort := prog.millis)(showPeriod(prog.period))
                  )
              }
            )
          )
        )
      )
    )

  private def studentList(c: Clas, students: List[Student.WithUser])(implicit ctx: Context) =
    div(cls := "students")(
      table(cls := "slist slist-pad sortable")(
        thead(
          tr(
            th(attr("data-sort-default") := "1")(trans.clas.nbStudents(students.size)),
            sortNumberTh(trans.rating()),
            sortNumberTh(trans.games()),
            sortNumberTh(trans.puzzles()),
            sortNumberTh(trans.clas.lastActiveDate()),
            th(iconTag("5")(title := trans.clas.managed.txt()))
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
                td(student.managed option iconTag("5")(title := trans.clas.managed.txt()))
              )
          }
        )
      )
    )

  private def studentTd(c: Clas, s: Student.WithUser)(implicit ctx: Context) =
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
}
