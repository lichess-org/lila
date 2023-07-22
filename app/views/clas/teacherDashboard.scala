package views.html.clas

import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, ClasInvite, ClasProgress, Student }
import lila.common.String.html.richText
import lila.rating.PerfType

object teacherDashboard:

  private[clas] def layout(
      c: Clas,
      students: List[Student.WithUserLike],
      active: String
  )(modifiers: Modifier*)(using PageContext) =
    bits.layout(c.name, Left(c withStudents students.map(_.student)))(
      cls := s"clas-show dashboard dashboard-teacher dashboard-teacher-$active",
      div(cls := "clas-show__top")(
        h1(dataIcon := licon.Group, cls := "text")(c.name),
        st.nav(cls := "dashboard-nav")(
          a(cls := active.active("overview"), href := clasRoutes.show(c.id.value))(trans.clas.overview()),
          a(cls := active.active("wall"), href := clasRoutes.wall(c.id.value))(trans.clas.news()),
          a(
            cls  := active.active("progress"),
            href := clasRoutes.progress(c.id.value, PerfType.Blitz.key, 7)
          )(trans.clas.progress()),
          a(cls := active.active("edit"), href := clasRoutes.edit(c.id.value))(trans.edit()),
          a(cls := active.active("students"), href := clasRoutes.students(c.id.value))(
            trans.clas.students()
          )
        )
      ),
      standardFlash,
      c.archived.map: archived =>
        div(cls := "clas-show__archived archived")(
          bits.showArchived(archived),
          postForm(action := clasRoutes.archive(c.id.value, v = false)):
            form3.submit(trans.clas.reopen(), icon = none)(cls := "confirm button-empty")
        ),
      modifiers
    )

  def overview(
      c: Clas,
      students: List[Student.WithUserPerfs]
  )(using PageContext) =
    layout(c, students, "overview")(
      div(cls := "clas-show__overview")(
        c.desc.trim.nonEmpty option div(cls := "clas-show__desc")(richText(c.desc)),
        div(cls := "clas-show__overview__manage")(
          clas.teachers(c),
          a(
            href     := clasRoutes.studentForm(c.id.value),
            cls      := "button button-clas text",
            dataIcon := licon.PlusButton
          )(trans.clas.addStudent())
        )
      ),
      if students.isEmpty
      then p(cls := "box__pad students__empty")(trans.clas.noStudents())
      else studentList(c, students)
    )

  def students(
      c: Clas,
      all: List[Student.WithUserPerfs],
      invites: List[ClasInvite]
  )(using PageContext) =
    layout(c, all.filter(_.student.isActive), "students"):
      val archived = all.filter(_.student.isArchived)
      val inviteBox =
        if invites.isEmpty
        then div(cls := "box__pad invites__empty")(h2(trans.clas.nbPendingInvitations(0)))
        else
          div(cls := "box__pad invites")(
            h2(trans.clas.nbPendingInvitations.pluralSame(invites.size)),
            table(cls := "slist"):
              tbody:
                invites.map: i =>
                  tr(
                    td(userIdLink(i.userId.some)),
                    td(i.realName),
                    td(if i.accepted has false then "Declined" else "Pending"),
                    td(momentFromNow(i.created.at)),
                    td:
                      postForm(action := clasRoutes.invitationRevoke(i._id.value)):
                        submitButton(cls := "button button-red button-empty")("Revoke")
                  )
          )
      val archivedBox =
        if archived.isEmpty
        then div(cls := "box__pad students__empty")(h2(trans.clas.noRemovedStudents()))
        else
          div(cls := "box__pad")(
            h2(trans.clas.removedStudents()),
            studentList(c, archived)
          )
      frag(inviteBox, archivedBox)

  def unreasonable(c: Clas, students: List[Student.WithUser], active: String)(using PageContext) =
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
      students: List[Student.WithUserPerf],
      progress: ClasProgress
  )(using PageContext) =
    layout(c, students, "progress")(
      progressHeader(c, progress.some),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(dataSortDefault)(
                trans.clas.variantXOverLastY(progress.perfType.trans, trans.nbDays.txt(progress.days)),
                thSortNumber(trans.rating()),
                thSortNumber(trans.clas.progress()),
                thSortNumber(if progress.isPuzzle then trans.puzzles() else trans.games()),
                if progress.isPuzzle then thSortNumber(trans.clas.winrate())
                else thSortNumber(trans.clas.timePlaying()),
                th
              )
            ),
            tbody(
              students.sortBy(_.user.username.value).map { case s @ Student.WithUserPerf(_, user, perf) =>
                val prog = progress(user withPerf perf)
                tr(
                  studentTd(c, s),
                  td(dataSort := perf.intRating, cls := "rating")(
                    perf.showRatingProvisional
                  ),
                  td(dataSort := prog.ratingProgress)(
                    ratingProgress(prog.ratingProgress) | trans.clas.na.txt()
                  ),
                  td(prog.nb),
                  if progress.isPuzzle then td(dataSort := prog.winRate)(prog.winRate, "%")
                  else td(dataSort := prog.millis)(showDuration(prog.duration)),
                  td(
                    if progress.isPuzzle then
                      a(href := routes.Puzzle.dashboard(progress.days, "home", user.username.value.some))(
                        trans.puzzle.puzzleDashboard()
                      )
                    else
                      a(href := routes.User.perfStat(user.username, progress.perfType.key))(
                        trans.perfStat.perfStats(progress.perfType.trans)
                      )
                  )
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
      basicCompletion: Map[UserId, Int],
      practiceCompletion: Map[UserId, Int],
      coordScores: Map[UserId, chess.ByColor[Int]]
  )(using PageContext) =
    layout(c, students, "progress")(
      progressHeader(c, none),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(dataSortDefault)(
                trans.clas.nbStudents.pluralSame(students.size),
                thSortNumber(trans.chessBasics()),
                thSortNumber(trans.practice()),
                thSortNumber(trans.coordinates.coordinates())
              )
            ),
            tbody(
              students.sortBy(_.user.username.value).map { case s @ Student.WithUser(_, user) =>
                val coord = coordScores.getOrElse(user.id, chess.ByColor(0, 0))
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

  private def progressHeader(c: Clas, progress: Option[ClasProgress])(using PageContext) =
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
              cls  := progress.map(_.perfType.key.value.active(pt.key.value)),
              href := clasRoutes.progress(c.id.value, pt.key, progress.fold(7)(_.days))
            )(pt.trans)
          },
          a(cls := progress.isEmpty.option("active"), href := clasRoutes.learn(c.id.value))(
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
                cls  := p.days.toString.active(days.toString),
                href := clasRoutes.progress(c.id.value, p.perfType.key, days)
              )(days)
            }
          )
        )
      }
    )

  private def studentList(c: Clas, students: List[Student.WithUserPerfs])(using Context) =
    div(cls := "students")(
      table(cls := "slist slist-pad sortable")(
        thead:
          tr(
            th(dataSortDefault)(trans.clas.nbStudents(students.size)),
            thSortNumber(trans.rating()),
            thSortNumber(trans.games()),
            thSortNumber(trans.puzzles()),
            thSortNumber(trans.clas.lastActiveDate()),
            th(iconTag(licon.Shield)(title := trans.clas.managed.txt()))
          )
        ,
        tbody:
          students.sortBy(_.user.username.value).map { case s @ Student.WithUserPerfs(student, user, perfs) =>
            tr(
              studentTd(c, s),
              td(dataSort := perfs.bestRating, cls := "rating")(perfs.bestAny3Perfs.map:
                showPerfRating(perfs, _)
              ),
              td(user.count.game.localize),
              td(perfs.puzzle.nb),
              td(dataSort := user.seenAt.map(_.toMillis.toString))(user.seenAt.map(momentFromNowOnce)),
              td(
                dataSort := (if student.managed then 1 else 0),
                student.managed option iconTag(licon.Shield)(title := trans.clas.managed.txt())
              )
            )
          }
      )
    )

  private def studentTd(c: Clas, s: Student.WithUserLike)(using Context) = td:
    a(href := clasRoutes.studentShow(c.id.value, s.user.username)):
      userSpan(
        s.user,
        name = span(s.user.username, em(s.student.realName)).some,
        withTitle = false
      )
