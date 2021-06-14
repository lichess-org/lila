package views.html.clas

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, ClasInvite, ClasProgress, Student }
import lila.common.String.html.richText
import lila.rating.PerfType
import lila.user.User

object teacherDashboard {

  private[clas] def layout(
      c: Clas,
      students: List[Student.WithUser],
      active: String
  )(modifiers: Modifier*)(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents students.map(_.student)))(
      cls := s"clas-show dashboard dashboard-teacher dashboard-teacher-$active",
      div(cls := "clas-show__top")(
        h1(dataIcon := "", cls := "text")(c.name),
        st.nav(cls := "dashboard-nav")(
          a(cls := active.active("overview"), href := routes.Clas.show(c.id.value))("Overview"),
          a(cls := active.active("wall"), href := routes.Clas.wall(c.id.value))("News"),
          a(
            cls := active.active("progress"),
            href := routes.Clas.progress(c.id.value, PerfType.Blitz.key, 7)
          )(trans.clas.progress()),
          a(cls := active.active("edit"), href := routes.Clas.edit(c.id.value))(trans.edit()),
          a(cls := active.active("students"), href := routes.Clas.students(c.id.value))(
            "Students"
          )
        )
      ),
      standardFlash(),
      c.archived map { archived =>
        div(cls := "clas-show__archived archived")(
          bits.showArchived(archived),
          postForm(action := routes.Clas.archive(c.id.value, v = false))(
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
            dataIcon := ""
          )(trans.clas.addStudent())
        )
      ),
      if (students.isEmpty)
        p(cls := "box__pad students__empty")(trans.clas.noStudents())
      else
        studentList(c, students)
    )

  def students(
      c: Clas,
      all: List[Student.WithUser],
      invites: List[ClasInvite]
  )(implicit ctx: Context) =
    layout(c, all.filter(_.student.isActive), "students") {
      val archived = all.filter(_.student.isArchived)
      val inviteBox =
        if (invites.isEmpty)
          div(cls := "box__pad invites__empty")(h2(trans.clas.nbPendingInvitations(0)))
        else
          div(cls := "box__pad invites")(
            h2(trans.clas.nbPendingInvitations.pluralSame(invites.size)),
            table(cls := "slist")(
              tbody(
                invites.map { i =>
                  tr(
                    td(userIdLink(i.userId.some)),
                    td(i.realName),
                    td(
                      if (i.accepted has false) "Declined" else "Pending"
                    ),
                    td(momentFromNow(i.created.at)),
                    td(
                      postForm(action := routes.Clas.invitationRevoke(i._id.value))(
                        submitButton(cls := "button button-red button-empty")("Revoke")
                      )
                    )
                  )
                }
              )
            )
          )
      val archivedBox =
        if (archived.isEmpty)
          div(cls := "box__pad students__empty")(h2(trans.clas.noRemovedStudents()))
        else
          div(cls := "box__pad")(
            h2(trans.clas.removedStudents()),
            studentList(c, archived)
          )
      frag(inviteBox, archivedBox)
    }

  def unreasonable(c: Clas, students: List[Student.WithUser], active: String)(implicit ctx: Context) =
    layout(c, students, active)(
      div(cls := "box__pad students__empty")(
        p(
          "This feature is only available for classes of ",
          lila.clas.Clas.maxStudents,
          " or fewer students."
        ),
        p(
          "This class has ",
          students.size,
          " students. You could maybe create more classes to split the students."
        )
      )
    )

  def progress(
      c: Clas,
      students: List[Student.WithUser],
      progress: ClasProgress
  )(implicit ctx: Context) =
    layout(c, students, "progress")(
      progressHeader(c, progress.some),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(dataSortDefault)(
                trans.clas.variantXOverLastY(progress.perfType.trans, trans.nbDays.txt(progress.days)),
                dataSortNumberTh(trans.rating()),
                dataSortNumberTh(trans.clas.progress()),
                dataSortNumberTh(if (progress.isPuzzle) trans.puzzles() else trans.games()),
                if (progress.isPuzzle) dataSortNumberTh(trans.clas.winrate())
                else dataSortNumberTh(trans.clas.timePlaying())
              )
            ),
            tbody(
              students.sortBy(_.user.username).map { case s @ Student.WithUser(_, user) =>
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

  def learn(
      c: Clas,
      students: List[Student.WithUser],
      basicCompletion: Map[User.ID, Int],
      practiceCompletion: Map[User.ID, Int],
      coordScores: Map[User.ID, chess.Color.Map[Int]]
  )(implicit ctx: Context) =
    layout(c, students, "progress")(
      progressHeader(c, none),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(dataSortDefault)(
                trans.clas.nbStudents.pluralSame(students.size),
                dataSortNumberTh(trans.chessBasics()),
                dataSortNumberTh(trans.practice()),
                dataSortNumberTh(trans.coordinates.coordinates())
              )
            ),
            tbody(
              students.sortBy(_.user.username).map { case s @ Student.WithUser(_, user) =>
                val coord = coordScores.getOrElse(user.id, chess.Color.Map(0, 0))
                tr(
                  studentTd(c, s),
                  td(dataSort := basicCompletion.getOrElse(user.id, 0))(
                    basicCompletion.getOrElse(user.id, 0).toString,
                    "%"
                  ),
                  td(dataSort := practiceCompletion.getOrElse(user.id, 0))(
                    practiceCompletion.getOrElse(user.id, 0).toString,
                    "%"
                  ),
                  td(dataSort := coord.white, cls := "coords")(
                    i(cls := "color-icon is white")(coord.white),
                    i(cls := "color-icon is black")(coord.black)
                  )
                )
              }
            )
          )
        )
      )
    )

  private def progressHeader(c: Clas, progress: Option[ClasProgress])(implicit ctx: Context) =
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
              cls := progress.map(_.perfType.key.active(pt.key)),
              href := routes.Clas.progress(c.id.value, pt.key, progress.fold(7)(_.days))
            )(pt.trans)
          },
          a(cls := progress.isEmpty.option("active"), href := routes.Clas.learn(c.id.value))(
            trans.learnMenu()
          )
        )
      ),
      progress.map { p =>
        div(cls := "progress-days")(
          label(trans.clas.overDays()),
          div(cls := "progress-choices")(
            List(1, 2, 3, 7, 10, 14, 21, 30, 60, 90).map { days =>
              a(
                cls := p.days.toString.active(days.toString),
                href := routes.Clas.progress(c.id.value, p.perfType.key, days)
              )(days)
            }
          )
        )
      }
    )

  private def studentList(c: Clas, students: List[Student.WithUser])(implicit ctx: Context) =
    div(cls := "students")(
      table(cls := "slist slist-pad sortable")(
        thead(
          tr(
            th(dataSortDefault)(trans.clas.nbStudents(students.size)),
            dataSortNumberTh(trans.rating()),
            dataSortNumberTh(trans.games()),
            dataSortNumberTh(trans.puzzles()),
            dataSortNumberTh(trans.clas.lastActiveDate()),
            th(iconTag("")(title := trans.clas.managed.txt()))
          )
        ),
        tbody(
          students.sortBy(_.user.username).map { case s @ Student.WithUser(student, user) =>
            tr(
              studentTd(c, s),
              td(dataSort := user.perfs.bestRating, cls := "rating")(user.best3Perfs.map {
                showPerfRating(user, _)
              }),
              td(user.count.game.localize),
              td(user.perfs.puzzle.nb),
              td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce)),
              td(
                dataSort := (if (student.managed) 1 else 0),
                student.managed option iconTag("")(title := trans.clas.managed.txt())
              )
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
