package controllers
package clas

import akka.stream.scaladsl.*
import play.api.data.Form
import play.api.mvc.*
import scalalib.model.Days

import lila.app.{ *, given }
import lila.clas.ClasForm.ClasData
import lila.clas.{ ClasBulk, ClasBulkForm, Student }
import lila.core.id.{ ClasId, ClasInviteId }
import lila.core.security.ClearPassword

final class Clas(env: Env, authC: Auth) extends LilaController(env):

  def index = Open: ctx ?=>
    NoBot:
      ctx.me
        .match
          case _ if getBool("home") => renderHome
          case None => renderHome
          case Some(me) if isGrantedOpt(_.Teacher) && !me.lameOrTroll =>
            Ok.async:
              env.clas.api.clas
                .of(me)
                .map:
                  views.clas.clas.teacherIndex(_, getBool("closed"))
          case Some(me) =>
            for
              hasClas <- fuccess(env.clas.studentCache.isStudent(me)) >>| couldBeTeacher.not
              res <-
                if hasClas
                then
                  for
                    ids <- env.clas.api.student.clasIdsOfUser(me)
                    classes <- env.clas.api.clas.byIds(ids)
                    res <- classes match
                      case List(single) => redirectTo(single).toFuccess
                      case many => Ok.page(views.clas.clas.studentIndex(many))
                  yield res
                else renderHome
            yield res
        .map(_.hasPersonalData)

  def teacher(username: UserStr) = Secure(_.Admin) { ctx ?=> _ ?=>
    FoundPage(meOrFetch(username)): teacher =>
      env.clas.api.clas
        .of(teacher)
        .map(views.mod.search.teacher(teacher.id, _))
    .map(_.hasPersonalData)
  }

  private def renderHome(using Context) =
    Ok.page(views.clas.ui.home)

  def form = Secure(_.Teacher) { ctx ?=> _ ?=>
    Ok.async(renderCreate(none)).map(_.hasPersonalData)
  }

  def create = SecureBody(_.Teacher) { ctx ?=> me ?=>
    NoTor:
      SafeTeacher:
        env.clas.forms.clas.create.flatMap:
          _.form
            .bindFromRequest()
            .fold(
              err => BadRequest.async(renderCreate(err.some)),
              data =>
                env.security.hcaptcha
                  .verify()
                  .flatMap: captcha =>
                    if captcha.ok
                    then env.clas.api.clas.create(data, me).map(redirectTo)
                    else BadRequest.async(renderCreate(data.some))
            )
  }

  private def renderCreate(from: Option[Form[ClasData] | ClasData])(using ctx: Context) =
    env.clas.forms.clas.create.map: baseForm =>
      views.clas.clas.create:
        from.fold(baseForm):
          case data: ClasData => baseForm.fill(data)
          case form: Form[ClasData] => baseForm.withForm(form)

  private def preloadStudentUsers(students: List[lila.clas.Student.WithUser]): Unit =
    env.user.lightUserApi.preloadUsers(students.map(_.user))

  def show(id: ClasId) = Auth { ctx ?=> me ?=>
    WithClassAny(id)(
      forTeacher = WithClass(id): clas =>
        for
          _ <- env.msg.systemMsg.twoFactorReminder(me)
          students <- env.clas.api.student.activeWithUsers(clas)
          _ = preloadStudentUsers(students)
          students <- env.clas.api.student.withPerfs(students)
          page <- renderPage(views.clas.teacherDashboard.overview(clas, students))
        yield Ok(page),
      forStudent = (clas, students) =>
        for
          teachers <- env.clas.api.clas.teachers(clas)
          _ = preloadStudentUsers(students)
          students <- env.clas.api.student.withPerfs(students)
          html <- env.clas.markdown.wallHtml(clas)
          page <- renderPage:
            views.clas.studentDashboard(
              clas,
              html,
              teachers,
              students
            )
        yield Ok(page),
      orDefault = _ =>
        isGranted(_.UserModView).so(FoundPage(env.clas.api.clas.byId(id)): clas =>
          env.clas.api.student.allWithUsers(clas).flatMap { students =>
            env.user.api.withPerfsAndEmails(students.map(_.user)).map {
              views.mod.search.clas(clas, _)
            }
          })
    )
  }

  private def WithClassAny(id: ClasId)(
      forTeacher: => Fu[Result],
      forStudent: (lila.clas.Clas, List[lila.clas.Student.WithUser]) => Fu[Result],
      orDefault: Context => Fu[Result] = notFound(using _)
  )(using ctx: Context, me: Me): Fu[Result] =
    isGranted(_.Teacher)
      .so(env.clas.api.clas.isTeacherOf(me, id))
      .flatMap:
        if _ then forTeacher
        else
          Found(env.clas.api.clas.byId(id)): clas =>
            env.clas.api.student.activeWithUsers(clas).flatMap { students =>
              if students.exists(_.student.is(me)) then forStudent(clas, students)
              else orDefault(ctx)
            }
      .map(_.hasPersonalData)

  def wall(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAny(id)(
      forTeacher = WithClass(id): clas =>
        Ok.async:
          for
            students <- env.clas.api.student.allWithUsers(clas)
            html <- env.clas.markdown.wallHtml(clas)
          yield views.clas.teacherDashboard.wall
            .show(clas, students, html)
      ,
      forStudent = (clas, _) => redirectTo(clas)
    )
  }

  def wallEdit(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      Ok.async:
        env.clas.api.student
          .activeWithUsers(clas)
          .map:
            views.clas.teacherDashboard.wall.edit(clas, _, env.clas.forms.clas.wall.fill(clas.wall))
  }

  def wallUpdate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      bindForm(env.clas.forms.clas.wall)(
        err =>
          BadRequest.async:
            env.clas.api.student.activeWithUsers(clas).map {
              views.clas.teacherDashboard.wall.edit(clas, _, err)
            }
        ,
        text =>
          env.clas.api.clas
            .updateWall(clas, text)
            .inject(Redirect(routes.Clas.wall(clas.id)).flashSuccess)
      )
  }

  def notifyStudents(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.student.activeWithUsers(clas).flatMap { students =>
        Reasonable(clas, students, "notify"):
          Ok.page:
            views.clas.teacherDashboard.notifyForm(clas, students, env.clas.forms.clas.notifyText)
      }
  }

  def notifyPost(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      bindForm(env.clas.forms.clas.notifyText)(
        err =>
          BadRequest.async:
            env.clas.api.student.activeWithUsers(clas).map {
              views.clas.teacherDashboard.notifyForm(clas, _, err)
            }
        ,
        text =>
          env.clas.api.student.activeWithUsers(clas).flatMap { students =>
            Reasonable(clas, students, "notify"):
              val call = routes.Clas.show(clas.id)
              val full = if text.contains(call.url) then text else s"$text\n\n${routeUrl(call)}"
              env.msg.api
                .multiPost(Source(students.map(_.user.id)), full)
                .addEffect(lila.mon.msg.clasBulk(clas.id).record(_))
                .inject(redirectTo(clas).flashSuccess)
          }
      )
  }

  def bulkActions(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      for
        data <- env.clas.bulk.load(clas)
        page <- renderPage(views.clas.teacherDashboard.bulkActions(data))
      yield Ok(page)
  }

  def bulkActionsPost(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      bindForm(ClasBulkForm.form)(
        _ => Redirect(routes.Clas.bulkActions(id)).flashFailure,
        data =>
          import ClasBulk.PostResponse.*
          for
            done <- env.clas.bulk.post(clas, data)
            redirect = Redirect(routes.Clas.bulkActions(id))
            res <- done match
              case Done => redirect.flashSuccess.toFuccess
              case Fail => redirect.flashFailure(s"Action ${data.action} not supported.").toFuccess
              case CloseAccounts(users) =>
                users
                  .sequentiallyVoid(env.api.accountTermination.disable(_, forever = false))
                  .inject(redirect.flashSuccess)
          yield res
      )
  }

  def students(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      for
        students <- env.clas.api.student.allWithUsers(clas)
        students <- env.clas.api.student.withPerfs(students)
        invites <- env.clas.api.invite.listPending(clas)
        login <- getBool("codes").so(env.clas.login.get(clas.id))
        page <- renderPage(views.clas.teacherDashboard.students(clas, students, invites, login))
      yield Ok(page)
  }

  def progress(id: ClasId, perfKey: PerfKey, days: Days) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.student.activeWithUsers(clas).flatMap { students =>
        Reasonable(clas, students, "progress"):
          for
            progress <- env.clas.progressApi(perfKey, days, students)
            students <- env.clas.api.student.withPerf(students, perfKey)
            page <- renderPage(views.clas.teacherDashboard.progress(clas, students, progress))
          yield Ok(page)
      }
  }

  def learn(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.student.activeWithUsers(clas).flatMap { students =>
        Reasonable(clas, students, "progress"):
          val studentIds = students.map(_.user.id)
          Ok.async:
            env.learn.api
              .completionPercent(studentIds)
              .zip(env.practice.api.progress.completionPercent(studentIds))
              .zip(env.coordinate.api.bestScores(studentIds))
              .map { case ((basic, practice), coords) =>
                views.clas.teacherDashboard.learn(clas, students, basic, practice, coords)
              }
      }
  }

  def edit(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      Ok.async:
        env.clas.api.student
          .activeWithUsers(clas)
          .map:
            views.clas.clas.edit(clas, _, env.clas.forms.clas.edit(clas))
  }

  def update(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      bindForm(env.clas.forms.clas.edit(clas))(
        err =>
          BadRequest.async:
            env.clas.api.student.activeWithUsers(clas).map {
              views.clas.clas.edit(clas, _, err)
            }
        ,
        data =>
          env.clas.api.clas.update(clas, data).map { clas =>
            redirectTo(clas).flashSuccess
          }
      )
  }

  def archive(id: ClasId, v: Boolean) = SecureBody(_.Teacher) { _ ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.clas.archive(clas, v).inject(redirectTo(clas).flashSuccess)
  }

  def studentForm(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    if getBool("gen") then env.clas.nameGenerator().orNotFound { Ok(_) }
    else
      WithClassAndStudents(id): (clas, students) =>
        for
          created <- ctx.req.flash
            .get("created")
            .map(_.split(' '))
            .so:
              case Array(userId, password) =>
                env.clas.api.student
                  .get(clas, UserId(userId))
                  .map2(lila.clas.Student.WithPassword(_, ClearPassword(password)))
              case _ => fuccess(none)
          nbStudents <- env.clas.api.student.count(clas.id)
          createForm <- env.clas.forms.student.generate
          inviteForm = env.clas.forms.student.invite(clas)
          page <- renderPage:
            views.clas.student.form(clas, students, inviteForm, createForm, nbStudents, created)
        yield Ok(page)
  }

  def studentCreate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    NoTor:
      Firewall:
        SafeTeacher:
          WithClassAndStudents(id): (clas, students) =>
            bindForm(env.clas.forms.student.create)(
              err =>
                BadRequest.async:
                  env.clas.api.student.count(clas.id).map {
                    views.clas.student
                      .form(clas, students, env.clas.forms.student.invite(clas), err, _, none)
                  }
              ,
              data =>
                env.clas.api.student.create(clas, data).map { s =>
                  Redirect(routes.Clas.studentForm(clas.id))
                    .flashing("created" -> s"${s.student.userId} ${s.password.value}")
                }
            )
  }

  def studentManyForm(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      for
        created <- ctx.req.flash
          .get("created")
          .so:
            _.split('/').toList
              .flatMap:
                _.split(' ') match
                  case Array(u, p) => (UserId(u), p).some
                  case _ => none
              .sequentially: (u, p) =>
                env.clas.api.student
                  .get(clas, u)
                  .map2(lila.clas.Student.WithPassword(_, ClearPassword(p)))
              .map(_.flatten)
        nbStudents <- env.clas.api.student.count(clas.id)
        form = env.clas.forms.student.manyCreate(lila.clas.Clas.maxStudents - nbStudents)
        page <- renderPage(views.clas.student.manyForm(clas, students, form, nbStudents, created))
      yield Ok(page)
  }

  def studentManyCreate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    NoTor:
      Firewall:
        SafeTeacher:
          WithClassAndStudents(id): (clas, students) =>
            env.clas.api.student.count(clas.id).flatMap { nbStudents =>
              bindForm(env.clas.forms.student.manyCreate(lila.clas.Clas.maxStudents - nbStudents))(
                err => BadRequest.page(views.clas.student.manyForm(clas, students, err, nbStudents, Nil)),
                data =>
                  env.clas.api.student.manyCreate(clas, data).flatMap { many =>
                    env.user.lightUserApi
                      .preloadMany(many.map(_.student.userId))
                      .inject(
                        Redirect(routes.Clas.studentManyForm(clas.id))
                          .flashing:
                            "created" -> many
                              .map: s =>
                                s"${s.student.userId} ${s.password.value}"
                              .mkString("/")
                      )
                  }
              )
            }
  }

  def studentInvite(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      bindForm(env.clas.forms.student.invite(clas))(
        err =>
          BadRequest.async:
            env.clas.api.student.count(clas.id).map {
              views.clas.student.form(clas, students, err, env.clas.forms.student.create, _, None)
            }
        ,
        data =>
          Found(env.user.repo.enabledById(data.username)): user =>
            env.clas.api.invite.create(clas, user, data.realName).map { feedback =>
              Redirect(routes.Clas.studentForm(clas.id)).flashing(feedback.flash(user.username))
            }
      )
  }

  def studentShow(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        for
          s <- env.clas.api.student.withPerfs(s)
          withManagingClas <- env.clas.api.student.withManagingClas(s, clas)
          activity <- env.activity.read.recentAndPreload(s.user)
          page <- renderPage(views.clas.student.show(clas, students, withManagingClas, activity))
        yield Ok(page)
  }

  def studentEdit(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        Ok.page:
          views.clas.student.edit(clas, students, s, env.clas.forms.student.edit(s.student))
  }

  def studentUpdate(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        bindForm(env.clas.forms.student.edit(s.student))(
          err => BadRequest.page(views.clas.student.edit(clas, students, s, err)),
          data =>
            env.clas.api.student.update(s.student, data).map { _ =>
              Redirect(routes.Clas.studentShow(clas.id, s.user.username)).flashSuccess
            }
        )
  }

  def studentArchive(id: ClasId, username: UserStr, v: Boolean) = Secure(_.Teacher) { _ ?=> me ?=>
    WithClass(id): clas =>
      WithStudent(clas, username): s =>
        env.clas.api.student
          .archive(clas, s.student.id, v)
          .inject(Redirect(routes.Clas.studentShow(clas.id, s.user.username)).flashSuccess)
  }

  def studentResetPassword(id: ClasId, username: UserStr) =
    Secure(_.Teacher) { _ ?=> me ?=>
      WithClass(id): clas =>
        WithStudent(clas, username): s =>
          for
            _ <- env.security.store.closeAllSessionsOf(s.user.id)
            password <- env.clas.api.student.resetPassword(s.student)
          yield Redirect(routes.Clas.studentShow(clas.id, s.user.username))
            .flashing("password" -> password.value)
    }

  def studentRelease(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then Ok.page(views.clas.student.release(clas, students, s, env.clas.forms.student.release))
        else Redirect(routes.Clas.studentShow(clas.id, s.user.username))
  }

  def studentReleasePost(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then
          env.security.forms.preloadEmailDns() >>
            bindForm(env.clas.forms.student.release)(
              err => BadRequest.page(views.clas.student.release(clas, students, s, err)),
              email =>
                val newUserEmail = lila.security.EmailConfirm.UserEmail(s.user.username, email)
                authC.EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimited):
                  env.security.emailChange
                    .send(s.user, newUserEmail.email)
                    .inject(Redirect(routes.Clas.studentShow(clas.id, s.user.username)).flashSuccess:
                      s"A confirmation email was sent to ${email}. ${s.student.realName} must click the link in the email to release the account.")
            )
        else Redirect(routes.Clas.studentShow(clas.id, s.user.username))
  }

  def studentClose(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then Ok.page(views.clas.student.close(clas, students, s))
        else Redirect(routes.Clas.studentShow(clas.id, s.user.username))
  }

  def studentClosePost(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { _ ?=> me ?=>
    WithClassAndStudents(id): (clas, _) =>
      WithStudent(clas, username): s =>
        if s.student.managed then
          for
            _ <- env.clas.api.student.deleteStudent(clas, s)
            _ <- env.api.accountTermination.disable(s.user, forever = false)
          yield redirectTo(clas).flashSuccess
        else if s.student.isArchived then
          for _ <- env.clas.api.student.deleteStudent(clas, s)
          yield redirectTo(clas).flashSuccess
        else redirectTo(clas)
  }

  def studentMove(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        for
          classes <- env.clas.api.clas.of(me)
          others = classes.filter(_.id != clas.id)
          res <- Ok.page(views.clas.student.move(clas, students, s, others))
        yield res
  }

  def studentMovePost(id: ClasId, username: UserStr, to: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, _) =>
      WithStudent(clas, username): s =>
        WithClass(to): toClas =>
          for _ <- env.clas.api.student.move(clas, s, toClas)
          yield Redirect(routes.Clas.show(clas.id)).flashSuccess
  }

  def becomeTeacher = AuthBody { ctx ?=> me ?=>
    couldBeTeacher.elseNotFound:
      val perm = lila.core.perm.Permission.Teacher.dbKey
      for _ <- me.roles.has(perm).not.so(env.user.repo.setRoles(me, perm :: me.roles).void)
      yield Redirect(routes.Clas.index)
  }

  private def couldBeTeacher(using ctx: Context): Fu[Boolean] = ctx.me.soUse: me ?=>
    if me.isBot then fuFalse
    else if ctx.kid.yes then fuFalse
    else if env.clas.hasClas then fuTrue
    else env.mod.logApi.wasUnteachered(me).not

  def invitation(id: ClasInviteId) = Auth { _ ?=> me ?=>
    FoundPage(env.clas.api.invite.view(id, me)): (invite, clas) =>
      views.clas.student.invite(clas, invite)
  }

  def invitationAccept(id: ClasInviteId) = AuthBody { ctx ?=> me ?=>
    bindForm(env.clas.forms.student.inviteAccept)(
      _ => Redirect(routes.Clas.invitation(id)).toFuccess,
      v =>
        if v then
          Found(env.clas.api.invite.accept(id, me)): student =>
            redirectTo(student.clasId)
        else env.clas.api.invite.decline(id).inject(Redirect(routes.Clas.invitation(id)))
    )
  }

  def invitationRevoke(id: ClasInviteId) = Secure(_.Teacher) { _ ?=> me ?=>
    Found(env.clas.api.invite.get(id)): invite =>
      WithClass(invite.clasId): clas =>
        env.clas.api.invite.delete(invite.id).inject(Redirect(routes.Clas.students(clas.id)))
  }

  def loginCreate(id: ClasId) = Secure(_.Teacher) { _ ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      for _ <- env.clas.login.create(clas, students)
      yield Redirect(s"${routes.Clas.students(clas.id)}?codes=1")
  }

  private def Reasonable(clas: lila.clas.Clas, students: List[lila.clas.Student.WithUser], active: String)(
      f: => Fu[Result]
  )(using Context): Fu[Result] =
    if students.sizeIs <= lila.clas.Clas.maxStudents then f
    else Unauthorized.page(views.clas.teacherDashboard.unreasonable(clas, students, active))

  private def WithClass(clasId: ClasId)(f: lila.clas.Clas => Fu[Result])(using Context, Me): Fu[Result] =
    Found(env.clas.api.clas.getAndView(clasId))(f).map(_.hasPersonalData)

  private def WithClassAndStudents(clasId: ClasId)(
      f: (lila.clas.Clas, List[lila.clas.Student]) => Fu[Result]
  )(using Context, Me): Fu[Result] =
    WithClass(clasId): c =>
      env.clas.api.student.activeOf(c).flatMap { f(c, _) }

  private def WithStudent(clas: lila.clas.Clas, username: UserStr)(
      f: lila.clas.Student.WithUser => Fu[Result]
  )(using Context): Fu[Result] =
    Found(meOrFetch(username)): user =>
      Found(env.clas.api.student.get(clas, user))(f).map(_.hasPersonalData)

  private def SafeTeacher(f: => Fu[Result])(using Context): Fu[Result] =
    if ctx.me.exists(!_.lameOrTroll) && ctx.noBot then f
    else Redirect(routes.Clas.index)

  private def redirectTo(c: lila.clas.Clas): Result = redirectTo(c.id)
  private def redirectTo(c: ClasId): Result = Redirect(routes.Clas.show(c))
