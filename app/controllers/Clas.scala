package controllers
package clas

import akka.stream.scaladsl.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }

import lila.clas.ClasInvite
import lila.clas.Clas.{ Id as ClasId }

final class Clas(env: Env, authC: Auth) extends LilaController(env):

  def index = Open: ctx ?=>
    NoBot:
      ctx.me match
        case _ if getBool("home") => renderHome
        case None                 => renderHome
        case Some(me) if isGrantedOpt(_.Teacher) && !me.lameOrTroll =>
          Ok.pageAsync:
            env.clas.api.clas.of(me).map {
              views.html.clas.clas.teacherIndex(_, getBool("closed"))
            }
        case Some(me) =>
          for
            hasClas <- fuccess(env.clas.studentCache.isStudent(me)) >>| !couldBeTeacher
            res <-
              if hasClas
              then
                for
                  ids     <- env.clas.api.student.clasIdsOfUser(me)
                  classes <- env.clas.api.clas.byIds(ids)
                  res <- classes match
                    case List(single) => redirectTo(single).toFuccess
                    case many         => Ok.page(views.html.clas.clas.studentIndex(many))
                yield res
              else renderHome
          yield res

  def teacher(username: UserStr) = Secure(_.Admin) { ctx ?=> _ ?=>
    FoundPage(env.user.repo byId username): teacher =>
      env.clas.api.clas.of(teacher) map {
        html.mod.search.teacher(teacher.id, _)
      }
  }

  private def renderHome(using Context) =
    Ok.page(views.html.clas.clas.home)

  def form = Secure(_.Teacher) { ctx ?=> _ ?=>
    Ok.page(html.clas.clas.create(env.clas.forms.clas.create))
  }

  def create = SecureBody(_.Teacher) { ctx ?=> me ?=>
    SafeTeacher:
      env.clas.forms.clas.create
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.clas.clas.create(err)),
          data => env.clas.api.clas.create(data, me.value) map redirectTo
        )
  }

  private def preloadStudentUsers(students: List[lila.clas.Student.WithUser]): Unit =
    env.user.lightUserApi.preloadUsers(students.map(_.user))

  def show(id: ClasId) = Auth { ctx ?=> me ?=>
    WithClassAny(id)(
      forTeacher = WithClass(id): clas =>
        for
          _        <- env.msg.twoFactorReminder(me)
          students <- env.clas.api.student.activeWithUsers(clas)
          _ = preloadStudentUsers(students)
          students <- env.clas.api.student.withPerfs(students)
          page     <- renderPage(views.html.clas.teacherDashboard.overview(clas, students))
        yield Ok(page),
      forStudent = (clas, students) =>
        for
          teachers <- env.clas.api.clas.teachers(clas)
          _ = preloadStudentUsers(students)
          students <- env.clas.api.student.withPerfs(students)
          page <- renderPage:
            views.html.clas.studentDashboard(clas, env.clas.markup(clas), teachers, students)
        yield Ok(page),
      orDefault = _ =>
        isGranted(_.UserModView) so
          FoundPage(env.clas.api.clas.byId(id)): clas =>
            env.clas.api.student.allWithUsers(clas) flatMap { students =>
              env.user.api.withEmails(students.map(_.user)) map {
                html.mod.search.clas(clas, _)
              }
            }
    )
  }

  private def WithClassAny(id: ClasId)(
      forTeacher: => Fu[Result],
      forStudent: (lila.clas.Clas, List[lila.clas.Student.WithUser]) => Fu[Result],
      orDefault: Context => Fu[Result] = notFound(using _)
  )(using ctx: Context, me: Me): Fu[Result] =
    isGranted(_.Teacher).so(env.clas.api.clas.isTeacherOf(me, id)) flatMap {
      if _ then forTeacher
      else
        Found(env.clas.api.clas.byId(id)): clas =>
          env.clas.api.student.activeWithUsers(clas) flatMap { students =>
            if students.exists(_.student is me) then forStudent(clas, students)
            else orDefault(ctx)
          }
    }

  def wall(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAny(id)(
      forTeacher = WithClass(id): clas =>
        Ok.pageAsync:
          env.clas.api.student.allWithUsers(clas) map {
            views.html.clas.wall.show(clas, env.clas.markup(clas), _)
          }
      ,
      forStudent = (clas, _) => redirectTo(clas)
    )
  }

  def wallEdit(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      Ok.pageAsync:
        env.clas.api.student.activeWithUsers(clas) map {
          html.clas.wall.edit(clas, _, env.clas.forms.clas.wall fill clas.wall)
        }
  }

  def wallUpdate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.forms.clas.wall
        .bindFromRequest()
        .fold(
          err =>
            BadRequest.pageAsync:
              env.clas.api.student.activeWithUsers(clas) map {
                html.clas.wall.edit(clas, _, err)
              }
          ,
          text =>
            env.clas.api.clas.updateWall(clas, text) inject
              Redirect(routes.Clas.wall(clas.id.value)).flashSuccess
        )
  }

  def notifyStudents(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.student.activeWithUsers(clas) flatMap { students =>
        Reasonable(clas, students, "notify"):
          Ok.page:
            html.clas.clas.notify(clas, students, env.clas.forms.clas.notifyText)
      }
  }

  def notifyPost(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.forms.clas.notifyText
        .bindFromRequest()
        .fold(
          err =>
            BadRequest.pageAsync:
              env.clas.api.student.activeWithUsers(clas) map {
                html.clas.clas.notify(clas, _, err)
              }
          ,
          text =>
            env.clas.api.student.activeWithUsers(clas) flatMap { students =>
              Reasonable(clas, students, "notify"):
                val url  = routes.Clas.show(clas.id.value).url
                val full = if text contains url then text else s"$text\n\n${env.net.baseUrl}$url"
                env.msg.api
                  .multiPost(Source(students.map(_.user.id)), full)
                  .addEffect: nb =>
                    lila.mon.msg.clasBulk(clas.id.value).record(nb)
                  .inject(redirectTo(clas).flashSuccess)
            }
        )
  }

  def students(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      for
        students <- env.clas.api.student.allWithUsers(clas)
        students <- env.clas.api.student.withPerfs(students)
        invites  <- env.clas.api.invite.listPending(clas)
        page     <- renderPage(views.html.clas.teacherDashboard.students(clas, students, invites))
      yield Ok(page)
  }

  def progress(id: ClasId, key: lila.rating.Perf.Key, days: Int) = Secure(_.Teacher) { ctx ?=> me ?=>
    lila.rating
      .PerfType(key)
      .so: perfType =>
        WithClass(id): clas =>
          env.clas.api.student.activeWithUsers(clas) flatMap { students =>
            Reasonable(clas, students, "progress"):
              for
                progress <- env.clas.progressApi(perfType, days, students)
                students <- env.clas.api.student.withPerf(students, perfType)
                page     <- renderPage(views.html.clas.teacherDashboard.progress(clas, students, progress))
              yield Ok(page)
          }
  }

  def learn(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.student.activeWithUsers(clas) flatMap { students =>
        Reasonable(clas, students, "progress"):
          val studentIds = students.map(_.user.id)
          Ok.pageAsync:
            env.learn.api.completionPercent(studentIds) zip
              env.practice.api.progress.completionPercent(studentIds) zip
              env.coordinate.api.bestScores(studentIds) map { case ((basic, practice), coords) =>
                views.html.clas.teacherDashboard.learn(clas, students, basic, practice, coords)
              }
      }
  }

  def edit(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      Ok.pageAsync:
        env.clas.api.student.activeWithUsers(clas) map {
          html.clas.clas.edit(clas, _, env.clas.forms.clas.edit(clas))
        }
  }

  def update(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClass(id): clas =>
      env.clas.forms.clas
        .edit(clas)
        .bindFromRequest()
        .fold(
          err =>
            BadRequest.pageAsync:
              env.clas.api.student.activeWithUsers(clas) map {
                html.clas.clas.edit(clas, _, err)
              }
          ,
          data =>
            env.clas.api.clas.update(clas, data) map { clas =>
              redirectTo(clas).flashSuccess
            }
        )
  }

  def archive(id: ClasId, v: Boolean) = SecureBody(_.Teacher) { _ ?=> me ?=>
    WithClass(id): clas =>
      env.clas.api.clas.archive(clas, me.value, v) inject
        redirectTo(clas).flashSuccess
  }

  def studentForm(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    if getBool("gen") then env.clas.nameGenerator() orNotFound { Ok(_) }
    else
      WithClassAndStudents(id): (clas, students) =>
        for
          created <- ctx.req.flash.get("created").map(_ split ' ').so {
            case Array(userId, password) =>
              env.clas.api.student
                .get(clas, UserId(userId))
                .map2(lila.clas.Student.WithPassword(_, lila.user.User.ClearPassword(password)))
            case _ => fuccess(none)
          }
          nbStudents <- env.clas.api.student.count(clas.id)
          createForm <- env.clas.forms.student.generate
          inviteForm = env.clas.forms.student.invite(clas)
          page <- renderPage:
            html.clas.student.form(clas, students, inviteForm, createForm, nbStudents, created)
        yield Ok(page)
  }

  def studentCreate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    NoTor:
      Firewall:
        SafeTeacher:
          WithClassAndStudents(id): (clas, students) =>
            env.clas.forms.student.create
              .bindFromRequest()
              .fold(
                err =>
                  BadRequest.pageAsync:
                    env.clas.api.student.count(clas.id) map {
                      html.clas.student.form(clas, students, env.clas.forms.student.invite(clas), err, _)
                    }
                ,
                data =>
                  env.clas.api.student.create(clas, data, me.value) map { s =>
                    Redirect(routes.Clas.studentForm(clas.id.value))
                      .flashing("created" -> s"${s.student.userId} ${s.password.value}")
                  }
              )
  }

  def studentManyForm(id: ClasId) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      for
        created <- ctx.req.flash.get("created").so {
          _.split('/').toList
            .flatMap:
              _.split(' ') match
                case Array(u, p) => (UserId(u), p).some
                case _           => none
            .map: (u, p) =>
              env.clas.api.student
                .get(clas, u)
                .map2(lila.clas.Student.WithPassword(_, lila.user.User.ClearPassword(p)))
            .parallel
            .map(_.flatten)
        }
        nbStudents <- env.clas.api.student.count(clas.id)
        form = env.clas.forms.student.manyCreate(lila.clas.Clas.maxStudents - nbStudents)
        page <- renderPage(html.clas.student.manyForm(clas, students, form, nbStudents, created))
      yield Ok(page)
  }

  def studentManyCreate(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    NoTor:
      Firewall:
        SafeTeacher:
          WithClassAndStudents(id): (clas, students) =>
            env.clas.api.student.count(clas.id) flatMap { nbStudents =>
              env.clas.forms.student
                .manyCreate(lila.clas.Clas.maxStudents - nbStudents)
                .bindFromRequest()
                .fold(
                  err => BadRequest.page(html.clas.student.manyForm(clas, students, err, nbStudents)),
                  data =>
                    env.clas.api.student.manyCreate(clas, data, me.value) flatMap { many =>
                      env.user.lightUserApi.preloadMany(many.map(_.student.userId)) inject
                        Redirect(routes.Clas.studentManyForm(clas.id.value))
                          .flashing:
                            "created" -> many
                              .map: s =>
                                s"${s.student.userId} ${s.password.value}"
                              .mkString("/")
                    }
                )
            }
  }

  def studentInvite(id: ClasId) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      env.clas.forms.student
        .invite(clas)
        .bindFromRequest()
        .fold(
          err =>
            BadRequest.pageAsync:
              env.clas.api.student.count(clas.id) map {
                html.clas.student.form(clas, students, err, env.clas.forms.student.create, _)
              }
          ,
          data =>
            Found(env.user.repo enabledById data.username): user =>
              import lila.clas.ClasInvite.{ Feedback as F }
              import lila.i18n.{ I18nKeys as trans }
              env.clas.api.invite.create(clas, user, data.realName) map { feedback =>
                Redirect(routes.Clas.studentForm(clas.id.value)).flashing:
                  feedback match
                    case F.Already => "success" -> trans.clas.xisNowAStudentOfTheClass.txt(user.username)
                    case F.Invited =>
                      "success" -> trans.clas.anInvitationHasBeenSentToX.txt(user.username)
                    case F.Found =>
                      "warning" -> trans.clas.xAlreadyHasAPendingInvitation.txt(user.username)
                    case F.CantMsgKid(url) =>
                      "warning" -> trans.clas.xIsAKidAccountWarning.txt(user.username, url)
              }
        )
  }

  def studentShow(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        for
          s                <- env.clas.api.student.withPerfs(s)
          withManagingClas <- env.clas.api.student.withManagingClas(s, clas)
          activity         <- env.activity.read.recentAndPreload(s.user)
          page <- renderPage(views.html.clas.student.show(clas, students, withManagingClas, activity))
        yield Ok(page)
  }

  def studentEdit(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        Ok.page:
          views.html.clas.student.edit(clas, students, s, env.clas.forms.student edit s.student)
  }

  def studentUpdate(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        env.clas.forms.student
          .edit(s.student)
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.clas.student.edit(clas, students, s, err)),
            data =>
              env.clas.api.student.update(s.student, data) map { _ =>
                Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).flashSuccess
              }
          )
  }

  def studentArchive(id: ClasId, username: UserStr, v: Boolean) = Secure(_.Teacher) { _ ?=> me ?=>
    WithClass(id): clas =>
      WithStudent(clas, username): s =>
        env.clas.api.student.archive(s.student.id, v) inject
          Redirect(routes.Clas.studentShow(clas.id.value, s.user.username.value)).flashSuccess
  }

  def studentResetPassword(id: ClasId, username: UserStr) =
    Secure(_.Teacher) { _ ?=> me ?=>
      WithClass(id): clas =>
        WithStudent(clas, username): s =>
          env.security.store.closeAllSessionsOf(s.user.id) >>
            env.clas.api.student.resetPassword(s.student) map { password =>
              Redirect(routes.Clas.studentShow(clas.id.value, s.user.username.value))
                .flashing("password" -> password.value)
            }
    }

  def studentRelease(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then Ok.page(views.html.clas.student.release(clas, students, s, env.clas.forms.student.release))
        else Redirect(routes.Clas.studentShow(clas.id.value, s.user.username))
  }

  def studentReleasePost(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then
          env.security.forms.preloadEmailDns() >>
            env.clas.forms.student.release
              .bindFromRequest()
              .fold(
                err => BadRequest.page(html.clas.student.release(clas, students, s, err)),
                email =>
                  val newUserEmail = lila.security.EmailConfirm.UserEmail(s.user.username, email)
                  authC.EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimited):
                    env.security.emailChange.send(s.user, newUserEmail.email) inject
                      Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).flashSuccess:
                        s"A confirmation email was sent to ${email}. ${s.student.realName} must click the link in the email to release the account."
              )
        else Redirect(routes.Clas.studentShow(clas.id.value, s.user.username))
  }

  def studentClose(id: ClasId, username: UserStr) = Secure(_.Teacher) { ctx ?=> me ?=>
    WithClassAndStudents(id): (clas, students) =>
      WithStudent(clas, username): s =>
        if s.student.managed
        then Ok.page(views.html.clas.student.close(clas, students, s))
        else Redirect(routes.Clas.studentShow(clas.id.value, s.user.username))
  }

  def studentClosePost(id: ClasId, username: UserStr) = SecureBody(_.Teacher) { _ ?=> me ?=>
    WithClassAndStudents(id): (clas, _) =>
      WithStudent(clas, username): s =>
        if s.student.managed then
          env.clas.api.student.closeAccount(s) >>
            env.api.accountClosure.close(s.user) inject redirectTo(clas).flashSuccess
        else if s.student.isArchived then
          env.clas.api.student.closeAccount(s) >>
            redirectTo(clas).flashSuccess
        else redirectTo(clas)
  }

  def becomeTeacher = AuthBody { ctx ?=> me ?=>
    couldBeTeacher.elseNotFound:
      val perm = lila.security.Permission.Teacher.dbKey
      (!me.roles.has(perm) so env.user.repo.setRoles(me, perm :: me.roles).void) inject
        Redirect(routes.Clas.index)
  }

  private def couldBeTeacher(using ctx: Context): Fu[Boolean] = ctx.me.soUse: me ?=>
    if me.isBot then fuFalse
    else if ctx.kid.yes then fuFalse
    else if env.clas.hasClas then fuTrue
    else !env.mod.logApi.wasUnteachered(me)

  def invitation(id: lila.clas.ClasInvite.Id) = Auth { _ ?=> me ?=>
    FoundPage(env.clas.api.invite.view(id, me)): (invite, clas) =>
      views.html.clas.invite.show(clas, invite)
  }

  def invitationAccept(id: ClasInvite.Id) = AuthBody { ctx ?=> me ?=>
    Form(single("v" -> Forms.boolean))
      .bindFromRequest()
      .fold(
        _ => Redirect(routes.Clas.invitation(id)).toFuccess,
        v =>
          if v then
            Found(env.clas.api.invite.accept(id, me)): student =>
              redirectTo(student.clasId)
          else env.clas.api.invite.decline(id) inject Redirect(routes.Clas.invitation(id))
      )
  }

  def invitationRevoke(id: lila.clas.ClasInvite.Id) = Secure(_.Teacher) { _ ?=> me ?=>
    Found(env.clas.api.invite.get(id)): invite =>
      WithClass(invite.clasId): clas =>
        env.clas.api.invite.delete(invite._id) inject Redirect(routes.Clas.students(clas.id.value))
  }

  private def Reasonable(clas: lila.clas.Clas, students: List[lila.clas.Student.WithUser], active: String)(
      f: => Fu[Result]
  )(using Context): Fu[Result] =
    if students.sizeIs <= lila.clas.Clas.maxStudents then f
    else Unauthorized.page(views.html.clas.teacherDashboard.unreasonable(clas, students, active))

  private def WithClass(clasId: ClasId)(f: lila.clas.Clas => Fu[Result])(using Context, Me): Fu[Result] =
    Found(env.clas.api.clas.getAndView(clasId))(f)

  private def WithClassAndStudents(clasId: ClasId)(
      f: (lila.clas.Clas, List[lila.clas.Student]) => Fu[Result]
  )(using Context, Me): Fu[Result] =
    WithClass(clasId): c =>
      env.clas.api.student.activeOf(c) flatMap { f(c, _) }

  private def WithStudent(clas: lila.clas.Clas, username: UserStr)(
      f: lila.clas.Student.WithUser => Fu[Result]
  )(using Context): Fu[Result] =
    Found(env.user.repo byId username): user =>
      Found(env.clas.api.student.get(clas, user))(f)

  private def SafeTeacher(f: => Fu[Result])(using Context): Fu[Result] =
    if ctx.me.exists(!_.lameOrTroll) && !ctx.isBot then f
    else Redirect(routes.Clas.index)

  private def redirectTo(c: lila.clas.Clas): Result = redirectTo(c.id)
  private def redirectTo(c: ClasId): Result         = Redirect(routes.Clas show c.value)
