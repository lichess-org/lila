package lila.clas
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain
import lila.core.perf.UserPerfs
import lila.core.i18n.Translate

final class DashboardUi(helpers: Helpers, ui: ClasUi)(using NetDomain)(
    perfsTd: UserPerfs => Translate ?=> Frag
):
  import helpers.{ *, given }

  object teacher:

    def layout(c: Clas, students: List[Student.WithUserLike], active: String)(using Context) = frag(
      div(cls := "clas-show__top")(
        h1(dataIcon := Icon.Group, cls := "text")(c.name),
        st.nav(cls := "dashboard-nav")(
          a(cls := active.active("overview"), href := routes.Clas.show(c.id.value))(trans.clas.overview()),
          a(cls := active.active("wall"), href := routes.Clas.wall(c.id.value))(trans.clas.news()),
          a(
            cls  := active.active("progress"),
            href := routes.Clas.progress(c.id.value, PerfKey.blitz, 7)
          )(trans.clas.progress()),
          a(cls := active.active("edit"), href := routes.Clas.edit(c.id.value))(trans.site.edit()),
          a(cls := active.active("students"), href := routes.Clas.students(c.id.value))(
            trans.clas.students()
          )
        )
      ),
      standardFlash,
      c.archived.map: archived =>
        div(cls := "clas-show__archived archived")(
          ui.showArchived(archived),
          postForm(action := routes.Clas.archive(c.id.value, v = false)):
            form3.submit(trans.clas.reopen(), icon = none)(cls := "confirm button-empty")
        )
    )

    def overview(c: Clas, students: List[Student.WithUserPerfs])(using PageContext) =
      frag(
        div(cls := "clas-show__overview")(
          c.desc.trim.nonEmpty.option(div(cls := "clas-show__desc")(richText(c.desc))),
          div(cls := "clas-show__overview__manage")(
            ui.teachers(c),
            a(
              href     := routes.Clas.studentForm(c.id.value),
              cls      := "button button-clas text",
              dataIcon := Icon.PlusButton
            )(trans.clas.addStudent())
          )
        ),
        if students.isEmpty
        then p(cls := "box__pad students__empty")(trans.clas.noStudents())
        else studentList(c, students)
      )

    def students(c: Clas, all: List[Student.WithUserPerfs], invites: List[ClasInvite])(using Context) =
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
                    td(if i.accepted.has(false) then "Declined" else "Pending"),
                    td(momentFromNow(i.created.at)),
                    td:
                      postForm(action := routes.Clas.invitationRevoke(i._id.value)):
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

    def progress(c: Clas, students: List[Student.WithUserPerf], progress: ClasProgress)(using PageContext) =
      frag(
        progressHeader(c, progress.some),
        div(cls := "students")(
          table(cls := "slist slist-pad sortable")(
            thead(
              tr(
                th(dataSortDefault)(
                  trans.clas.variantXOverLastY(progress.perfType.trans, trans.site.nbDays.txt(progress.days)),
                  thSortNumber(trans.site.rating()),
                  thSortNumber(trans.clas.progress()),
                  thSortNumber(if progress.isPuzzle then trans.site.puzzles() else trans.site.games()),
                  if progress.isPuzzle then thSortNumber(trans.clas.winrate())
                  else thSortNumber(trans.clas.timePlaying()),
                  th
                )
              ),
              tbody(
                students.sortBy(_.user.username.value).map { case s @ Student.WithUserPerf(_, user, perf) =>
                  val prog = progress(user.withPerf(perf))
                  tr(
                    studentTd(c, s),
                    td(dataSort := perf.intRating, cls := "rating")(perf.glicko.display),
                    td(dataSort := prog.ratingProgress)(
                      ratingProgress(prog.ratingProgress) | trans.clas.na.txt()
                    ),
                    td(prog.nb),
                    if progress.isPuzzle then td(dataSort := prog.winRate)(prog.winRate, "%")
                    else td(dataSort := prog.millis)(lila.core.i18n.translateDuration(prog.duration)),
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
    )(using Context) = frag(
      progressHeader(c, none),
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead(
            tr(
              th(dataSortDefault)(
                trans.clas.nbStudents.pluralSame(students.size),
                thSortNumber(trans.site.chessBasics()),
                thSortNumber(trans.site.practice()),
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

    private def progressHeader(c: Clas, progress: Option[ClasProgress])(using Context) =
      div(cls := "progress")(
        div(cls := "progress-perf")(
          label(trans.site.variant()),
          div(cls := "progress-choices")(
            List(
              PerfKey.bullet,
              PerfKey.blitz,
              PerfKey.rapid,
              PerfKey.classical,
              PerfKey.correspondence,
              PerfKey.puzzle
            ).map { pk =>
              a(
                cls  := progress.map(_.perfType.key.value.active(pk.value)),
                href := routes.Clas.progress(c.id.value, pk, progress.fold(7)(_.days))
              )(pk.perfTrans)
            },
            a(cls := progress.isEmpty.option("active"), href := routes.Clas.learn(c.id.value))(
              trans.site.learnMenu()
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
                  href := routes.Clas.progress(c.id.value, p.perfType.key, days)
                )(days)
              }
            )
          )
        }
      )

    def unreasonable(students: Int)(using PageContext) =
      div(cls := "box__pad students__empty")(
        p(
          "This feature is only available for classes of ",
          lila.clas.Clas.maxStudents,
          " or fewer students."
        ),
        p(
          "This class has ",
          students,
          " students. You could maybe create more classes to split the students."
        )
      )

    private def studentList(c: Clas, students: List[Student.WithUserPerfs])(using Context) =
      div(cls := "students")(
        table(cls := "slist slist-pad sortable")(
          thead:
            tr(
              th(dataSortDefault)(trans.clas.nbStudents(students.size)),
              thSortNumber(trans.site.rating()),
              thSortNumber(trans.site.games()),
              thSortNumber(trans.site.puzzles()),
              thSortNumber(trans.clas.lastActiveDate()),
              th(iconTag(Icon.Shield)(title := trans.clas.managed.txt()))
            )
          ,
          tbody:
            students.sortBy(_.user.username.value).map {
              case s @ Student.WithUserPerfs(student, user, perfs) =>
                tr(
                  studentTd(c, s),
                  perfsTd(perfs),
                  td(user.count.game.localize),
                  td(perfs.puzzle.nb),
                  td(dataSort := user.seenAt.map(_.toMillis.toString))(user.seenAt.map(momentFromNowOnce)),
                  td(
                    dataSort := (if student.managed then 1 else 0),
                    student.managed.option(iconTag(Icon.Shield)(title := trans.clas.managed.txt()))
                  )
                )
            }
        )
      )

    private def studentTd(c: Clas, s: Student.WithUserLike)(using Context) = td:
      a(href := routes.Clas.studentShow(c.id.value, s.user.username)):
        userSpan(
          s.user,
          name = span(s.user.username, em(s.student.realName)).some,
          withTitle = false
        )

  object student:

    def apply(
        c: Clas,
        wall: Html,
        teachers: List[User],
        students: List[Student.WithUserPerfs]
    )(using Context) =
      frag(
        div(cls := "clas-show__top")(
          h1(dataIcon := Icon.Group, cls := "text")(c.name),
          c.desc.trim.nonEmpty.option(div(cls := "clas-show__desc")(richText(c.desc)))
        ),
        c.archived.map { archived =>
          div(cls := "box__pad")(
            div(cls := "clas-show__archived archived")(ui.showArchived(archived))
          )
        },
        table(cls := "slist slist-pad teachers")(
          thead:
            tr(
              th(trans.clas.nbTeachers(teachers.size)),
              th,
              th
            )
          ,
          tbody:
            teachers.map: user =>
              tr(
                td(
                  userLink(
                    user,
                    name = span(
                      strong(user.username),
                      user.profile.flatMap(_.nonEmptyRealName).map { em(_) }
                    ).some,
                    withTitle = false
                  )
                ),
                td(
                  user.seenAt.map: seen =>
                    trans.site.lastSeenActive(momentFromNow(seen))
                ),
                challengeTd(user)
              )
        ),
        c.wall.value.nonEmpty.option(div(cls := "box__pad clas-wall")(rawHtml(wall))),
        div(cls := "students")(studentList(students))
      )

    private def studentList(students: List[Student.WithUserPerfs])(using Context) =
      table(cls := "slist slist-pad sortable")(
        thead:
          tr(
            th(dataSortDefault)(trans.clas.nbStudents(students.size)),
            thSortNumber(trans.site.rating()),
            thSortNumber(trans.site.games()),
            thSortNumber(trans.site.puzzles()),
            th
          )
        ,
        tbody:
          students.sortBy(-_.user.seenAt.so(_.toMillis)).map {
            case Student.WithUserPerfs(student, user, perfs) =>
              tr(
                td(
                  userLink(
                    user,
                    name = span(
                      strong(user.username),
                      em(student.realName)
                    ).some,
                    withTitle = false
                  )
                ),
                perfsTd(perfs),
                td(user.count.game.localize),
                td(perfs.puzzle.nb.localize),
                challengeTd(user)
              )
          }
      )

    private def challengeTd(user: User)(using ctx: Context) =
      if ctx.is(user) then td
      else
        val online = isOnline(user.id)
        td(
          a(
            dataIcon := Icon.Swords,
            cls      := List("button button-empty text" -> true, "disabled" -> !online),
            title    := trans.challenge.challengeToPlay.txt(),
            href     := online.option(s"${routes.Lobby.home}?user=${user.username}#friend")
          )(trans.site.play())
        )
