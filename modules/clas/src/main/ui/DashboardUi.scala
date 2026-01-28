package lila.clas
package ui

import play.api.data.Form
import scalalib.model.Days

import lila.core.config.NetDomain
import lila.core.perf.UserPerfs
import lila.rating.UserPerfsExt.{ bestAny3Perfs, bestRating }
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class DashboardUi(helpers: Helpers, ui: ClasUi)(using NetDomain):
  import helpers.{ *, given }
  import ui.ClasPage

  object teacher:

    def TeacherPage(
        c: Clas,
        students: List[Student.WithUserLike],
        active: String
    )(modifiers: AttrPair*)(using Context): Page =
      val allModifiers =
        List(cls := s"clas-show dashboard dashboard-teacher dashboard-teacher-$active") ++ modifiers
      ClasPage(c.name, Left(c.withStudents(students.map(_.student))))(
        allModifiers*
      ).prepend(atTheTop(c, active))

    private def atTheTop(c: Clas, active: String)(using Context) =
      frag(
        div(cls := "clas-show__top")(
          h1(dataIcon := Icon.Group, cls := "text")(c.name),
          st.nav(cls := "dashboard-nav")(
            a(cls := active.active("overview"), href := routes.Clas.show(c.id))(trans.clas.overview()),
            a(cls := active.active("wall"), href := routes.Clas.wall(c.id))(trans.clas.news()),
            a(
              cls := active.active("progress"),
              href := routes.Clas.progress(c.id, PerfKey.blitz, Days(7))
            )(trans.clas.progress()),
            a(cls := active.active("edit"), href := routes.Clas.edit(c.id))(trans.site.edit()),
            a(cls := active.active("students"), href := routes.Clas.students(c.id))(
              trans.clas.students()
            )
          )
        ),
        standardFlash,
        c.archived.map: archived =>
          div(cls := "clas-show__archived archived")(
            ui.showArchived(archived),
            postForm(action := routes.Clas.archive(c.id, v = false)):
              form3.submit(trans.clas.reopen(), icon = none)(cls := "yes-no-confirm button-empty")
          )
      )

    object wall:

      def show(c: Clas, students: List[Student.WithUser], html: Html)(using Context) =
        TeacherPage(c, students.filter(_.student.isActive), "wall")():
          frag(
            div(cls := "clas-wall__actions")(
              a(
                dataIcon := Icon.Pencil,
                href := routes.Clas.wallEdit(c.id),
                cls := "button button-clas text"
              )(
                trans.clas.editNews()
              ),
              a(
                dataIcon := Icon.Envelope,
                href := routes.Clas.notifyStudents(c.id),
                cls := "button button-clas text"
              )(
                trans.clas.notifyAllStudents()
              )
            ),
            if c.wall.value.isEmpty then
              div(cls := "box__pad clas-wall clas-wall--empty")(trans.clas.nothingHere())
            else div(cls := "box__pad clas-wall")(html)
          )

      def edit(c: Clas, students: List[Student.WithUser], form: Form[?])(using Context) =
        TeacherPage(c, students.filter(_.student.isActive), "wall")():
          frag(
            div(cls := "box-pad clas-wall__edit")(
              p(
                strong(trans.clas.newsEdit1()),
                ul(
                  li(trans.clas.newsEdit2()),
                  li(trans.clas.newsEdit3()),
                  li(markdownIsAvailable)
                )
              ),
              postForm(cls := "form3", action := routes.Clas.wallUpdate(c.id))(
                form3.globalError(form),
                form3.group(
                  form("wall"),
                  trans.clas.classNews()
                )(form3.textarea(_)(rows := 20)),
                form3.actions(
                  a(href := routes.Clas.wall(c.id))(trans.site.cancel()),
                  form3.submit(trans.site.apply())
                )
              )
            )
          )

    def overview(c: Clas, students: List[Student.WithUserPerfs], tournaments: Option[Frag])(using Context) =
      TeacherPage(c, students, "overview")():
        frag(
          div(cls := "clas-show__overview")(
            c.desc.trim.nonEmpty.option(div(cls := "clas-show__desc")(richText(c.desc))),
            div(cls := "clas-show__overview__manage")(
              div(cls := "clas-teachers")(
                trans.clas.teachersX(fragList(c.teachers.toList.map(t => userIdLink(t.some))))
              ),
              c.teamId.map: teamId =>
                div(cls := "clas-team"):
                  a(href := routes.Team.show(teamId), cls := "text", dataIcon := Icon.Group)(
                    trans.team.team()
                  )
            )
          ),
          tournaments,
          if students.isEmpty
          then p(cls := "box__pad students__empty")(trans.clas.noStudents())
          else studentList(c, students)
        )

    def bulkActions(data: ClasBulk.PageData)(using Context) =
      import data.*
      val classButtons: Frag = otherClasses.map: toClass =>
        form3.submit(toClass.name, icon = Icon.InternalArrow.some, ("action", s"move-to-${toClass.id}").some)(
          cls := "yes-no-confirm button-blue button-empty button-no-upper",
          title := trans.clas.moveToClass.txt(toClass.name)
        )
      val doubleHelp: Tag =
        div(cls := "clas-show__bulk__helps form-group")(
          div(cls := "clas-show__bulk__help")(
            strong("Important! Make sure you understand how this works:"),
            p(
              "Edit the list above to keep some students selected for a bulk action.",
              br,
              "All the students remaining in the list will be affected by the action.",
              br,
              "The students you removed from the list will remain in the class, unchanged."
            )
          ),
          div(cls := "clas-show__bulk__help")(
            "Choose the action to perform on the students listed above:"
          )
        )
      TeacherPage(c, all.filter(_.student.isActive), "students")():
        div(cls := "clas-show__body clas-show__bulk")(
          postForm(cls := "form3", action := routes.Clas.bulkActionsPost(c.id))(
            form3.fieldset("Active students", toggle = true.some)(cls := "box-pad")(
              form3.group(
                form("activeStudents"),
                frag("Active students")
              )(form3.textarea(_)(rows := 12)),
              doubleHelp,
              div(cls := "form-group")(
                form3.submit("Archive", icon = none, ("action", "archive").some)(
                  cls := "yes-no-confirm button-blue button-empty"
                ),
                br,
                classButtons
              )
            ),
            form3.fieldset("Archived students", toggle = all.exists(_.student.isArchived).some)(
              cls := "box-pad"
            )(
              form3.group(
                form("archivedStudents"),
                frag("Archived students")
              )(form3.textarea(_)(rows := 7)),
              doubleHelp(
                "BEWARE: removing a student with managed account will close the account permanently."
              ),
              div(cls := "form-group")(
                form3.submit("Restore", icon = none, ("action", "restore").some)(
                  cls := "yes-no-confirm button-blue button-empty"
                ),
                form3.submit("Remove", icon = Icon.Trash.some, ("action", "remove").some)(
                  cls := "yes-no-confirm button-red button-empty"
                )
              )
            ),
            form3.fieldset("Pending invites", toggle = form("invites").value.exists(_.nonEmpty).some)(
              cls := "box-pad"
            )(
              form3.group(
                form("invites"),
                frag("Invites")
              )(form3.textarea(_)(rows := 7)),
              div(cls := "form-group")(
                form3.submit("Delete", icon = Icon.Trash.some, ("action", "delete-invites").some)(
                  cls := "yes-no-confirm button-red button-empty"
                )
              )
            )
          ),
          form3.actions(
            a(href := routes.Clas.students(c.id))(trans.site.cancel())
          )
        )

    def students(
        c: Clas,
        all: List[Student.WithUserPerfs],
        invites: List[ClasInvite],
        login: Option[ClasLogin]
    )(using Context) =
      TeacherPage(c, all.filter(_.student.isActive), "students")():
        val archived = all.filter(_.student.isArchived)
        div(cls := "clas-show__body")(login match
          case Some(l) => renderLogin(all.map(_.student), l)
          case None =>
            frag(
              div(cls := "clas-show__actions")(
                a(
                  href := routes.Clas.studentForm(c.id),
                  cls := "button button-clas text",
                  dataIcon := Icon.PlusButton
                )(trans.clas.addStudent()),
                a(
                  href := routes.Clas.bulkActions(c.id),
                  cls := "button button-clas text",
                  dataIcon := Icon.Tools
                )("Bulk actions"),
                postForm(action := routes.Clas.loginCreate(c.id))(
                  submitButton(cls := "button button-clas text", dataIcon := Icon.Group)(
                    trans.clas.quickLoginCodes()
                  )
                )
              ),
              div(cls := "invites")(
                h2(trans.clas.nbPendingInvitations.pluralSame(invites.size)),
                invites.nonEmpty.option:
                  table(cls := "slist"):
                    tbody:
                      invites.map: i =>
                        tr(
                          td(userIdLink(i.userId.some)),
                          td(i.realName),
                          td(
                            if i.accepted.has(false) then trans.clas.declined.txt()
                            else trans.clas.pending.txt()
                          ),
                          td(momentFromNow(i.created.at)),
                          td:
                            postForm(action := routes.Clas.invitationRevoke(i.id)):
                              submitButton(cls := "button button-red button-empty")(trans.site.delete())
                        )
              ),
              if archived.isEmpty
              then div(h2(trans.clas.noRemovedStudents()))
              else
                div(
                  h2(trans.clas.removedStudents()),
                  studentList(c, archived)
                )
            ))

    private def renderLogin(students: List[Student], login: ClasLogin)(using Context) =
      val url = routeUrl(routes.Clas.index)
      div(cls := "clas-login")(
        div(cls := "clas-login__title")(
          h2(trans.clas.quickLoginCodes()),
          span(cls := "clas-login__expiration"):
            trans.clas.expirationInMomentFromNow(momentFromNow(login.expiresAt))
        ),
        div(
          p(trans.clas.quickLoginCodesDesc1(a(href := url)(url))),
          p(trans.clas.quickLoginCodesDesc2())
        ),
        div(cls := "clas-login__cards"):
          for
            student <- students.filter(_.isActive).sortBy(!_.managed)
            c = login.codes.find(_.user == student.userId)
          yield div(cls := "clas-login__card")(
            h3(student.realName),
            userIdLink(student.userId.some, withOnline = false),
            c.map(_.code).fold(iconTag(Icon.X, "not managed"))(code(_))
          )
      )

    def progress(c: Clas, students: List[Student.WithUserPerf], progress: ClasProgress)(using Context) =
      TeacherPage(c, students, "progress")():
        frag(
          progressHeader(c, progress.some),
          div(cls := "students")(
            table(cls := "slist slist-pad sortable")(
              thead(
                tr(
                  th(dataSortDefault)(
                    trans.clas
                      .variantXOverLastY(progress.perfType.trans, trans.site.nbDays.txt(progress.days)),
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
                      td(dataSort := prog.ratingProgress.value)(
                        ratingProgress(prog.ratingProgress) | trans.clas.na.txt()
                      ),
                      td(prog.nb),
                      if progress.isPuzzle then td(dataSort := prog.winRate)(prog.winRate, "%")
                      else td(dataSort := prog.millis)(lila.core.i18n.translateDuration(prog.duration)),
                      td(
                        if progress.isPuzzle then
                          a(href := routes.Puzzle.dashboard(progress.days, "home", user.username.some))(
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
    )(using Context) =
      TeacherPage(c, students, "progress")():
        frag(
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

    private val daysList = Days.from(List(1, 2, 3, 7, 10, 14, 21, 30, 60, 90))

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
                cls := progress.map(_.perfType.key.value.active(pk.value)),
                href := routes.Clas.progress(c.id, pk, progress.fold(Days(7))(_.days))
              )(pk.perfTrans)
            },
            a(cls := progress.isEmpty.option("active"), href := routes.Clas.learn(c.id))(
              trans.site.learnMenu()
            )
          )
        ),
        progress.map: p =>
          div(cls := "progress-days")(
            label(trans.clas.overDays()),
            div(cls := "progress-choices")(
              daysList.map: days =>
                a(
                  cls := p.days.toString.active(days.toString),
                  href := routes.Clas.progress(c.id, p.perfType.key, days)
                )(days)
            )
          )
      )

    def notifyForm(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[?])(using Context) =
      TeacherPage(c, students, "wall")()(
        div(cls := "box-pad clas-wall__edit")(
          p(
            strong(trans.clas.sendAMessage()),
            br,
            trans.clas.aLinkToTheClassWillBeAdded()
          ),
          postForm(cls := "form3", action := routes.Clas.notifyPost(c.id))(
            form3.globalError(form),
            form3.group(
              form("text"),
              frag(trans.site.message())
            )(form3.textarea(_)(rows := 3)),
            form3.actions(
              a(href := routes.Clas.wall(c.id))(trans.site.cancel()),
              form3.submit(trans.site.send())
            )
          )
        )
      )

    def unreasonable(c: Clas, students: List[Student.WithUser], active: String)(using Context) =
      TeacherPage(c, students, active)():
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
      a(href := routes.Clas.studentShow(c.id, s.user.username)):
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
        students: List[Student.WithUserPerfs],
        tournaments: Option[Frag]
    )(using Context) =
      ClasPage(c.name, Left(c.withStudents(Nil)))(cls := "clas-show dashboard dashboard-student"):
        frag(
          div(cls := "clas-show__top")(
            h1(dataIcon := Icon.Group, cls := "text")(c.name),
            c.desc.trim.nonEmpty.option(div(cls := "clas-show__desc")(richText(c.desc)))
          ),
          standardFlash,
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
          tournaments,
          c.wall.value.nonEmpty.option(div(cls := "box__pad clas-wall")(wall)),
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
        val online = isOnline.exec(user.id)
        td(
          a(
            dataIcon := Icon.Swords,
            cls := List("button button-empty text" -> true, "disabled" -> !online),
            title := trans.challenge.challengeToPlay.txt(),
            href := online.option(s"${routes.Lobby.home}?user=${user.username}#friend")
          )(trans.site.play())
        )

  private def perfsTd(perfs: UserPerfs)(using Context) =
    td(dataSort := perfs.bestRating, cls := "rating")(
      perfs.bestAny3Perfs.map(showPerfRating(perfs, _))
    )
