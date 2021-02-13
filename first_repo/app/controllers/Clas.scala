package controllers

import akka.stream.scaladsl._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import views._

import lila.api.Context
import lila.app._

final class Clas(
    env: Env,
    authC: Auth
) extends LilaController(env) {

  def index =
    Open { implicit ctx =>
      ctx.me match {
        case _ if getBool("home") => renderHome
        case None                 => renderHome
        case Some(me) if isGranted(_.Teacher) && !me.lameOrTroll =>
          env.clas.api.clas.of(me) map { classes =>
            Ok(views.html.clas.clas.teacherIndex(classes))
          }
        case Some(me) =>
          if (env.clas.studentCache.isStudent(me.id))
            env.clas.api.student.clasIdsOfUser(me.id) flatMap
              env.clas.api.clas.byIds map {
                case List(single) => Redirect(routes.Clas.show(single.id.value))
                case many         => Ok(views.html.clas.clas.studentIndex(many))
              }
          else renderHome
      }
    }

  private def renderHome(implicit ctx: Context) =
    fuccess {
      pageHit
      Ok(views.html.clas.clas.home)
    }

  def form =
    Secure(_.Teacher) { implicit ctx => _ =>
      Ok(html.clas.clas.create(env.clas.forms.clas.create)).fuccess
    }

  def create =
    SecureBody(_.Teacher) { implicit ctx => me =>
      SafeTeacher {
        env.clas.forms.clas.create
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err => BadRequest(html.clas.clas.create(err)).fuccess,
            data =>
              env.clas.api.clas.create(data, me) map { clas =>
                Redirect(routes.Clas.show(clas.id.value))
              }
          )
      }
    }

  private def preloadStudentUsers(students: List[lila.clas.Student.WithUser]): Unit =
    env.user.lightUserApi.preloadUsers(students.map(_.user))

  def show(id: String) =
    Auth { implicit ctx => me =>
      WithClassAny(id, me)(
        forTeacher = WithClass(me, id) { clas =>
          env.clas.api.student.activeWithUsers(clas) map { students =>
            preloadStudentUsers(students)
            views.html.clas.teacherDashboard.overview(clas, students)
          }
        },
        forStudent = (clas, students) =>
          env.clas.api.clas.teachers(clas) map { teachers =>
            preloadStudentUsers(students)
            val wall = scalatags.Text.all.raw(env.clas.markup(clas.wall))
            Ok(views.html.clas.studentDashboard(clas, wall, teachers, students))
          }
      )
    }

  private def WithClassAny(id: String, me: lila.user.User)(
      forTeacher: => Fu[Result],
      forStudent: (lila.clas.Clas, List[lila.clas.Student.WithUser]) => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    isGranted(_.Teacher).??(env.clas.api.clas.isTeacherOf(me, lila.clas.Clas.Id(id))) flatMap {
      case true => forTeacher
      case _ =>
        env.clas.api.clas.byId(lila.clas.Clas.Id(id)) flatMap {
          _ ?? { clas =>
            env.clas.api.student.activeWithUsers(clas) flatMap { students =>
              students.exists(_.student is me) ?? forStudent(clas, students)
            }
          }
        }
    }

  def wall(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClassAny(id, me)(
        forTeacher = WithClass(me, id) { clas =>
          env.clas.api.student.allWithUsers(clas) map { students =>
            val wall = scalatags.Text.all.raw(env.clas.markup(clas.wall))
            views.html.clas.wall.show(clas, wall, students)
          }
        },
        forStudent = (clas, _) => Redirect(routes.Clas.show(clas.id.value)).fuccess
      )
    }

  def wallEdit(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.api.student.activeWithUsers(clas) map { students =>
          Ok(html.clas.wall.edit(clas, students, env.clas.forms.clas.wall fill clas.wall))
        }
      }
    }

  def wallUpdate(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.forms.clas.wall
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err =>
              env.clas.api.student.activeWithUsers(clas) map { students =>
                BadRequest(html.clas.wall.edit(clas, students, err))
              },
            text =>
              env.clas.api.clas.updateWall(clas, text) inject
                Redirect(routes.Clas.wall(clas.id.value)).flashSuccess
          )
      }
    }

  def notifyStudents(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.api.student.activeWithUsers(clas) flatMap { students =>
          Reasonable(clas, students, "notify") {
            Ok(html.clas.clas.notify(clas, students, env.clas.forms.clas.notifyText)).fuccess
          }
        }
      }
    }

  def notifyPost(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.forms.clas.notifyText
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err =>
              env.clas.api.student.activeWithUsers(clas) map { students =>
                BadRequest(html.clas.clas.notify(clas, students, err))
              },
            text =>
              env.clas.api.student.activeWithUsers(clas) flatMap { students =>
                Reasonable(clas, students, "notify") {
                  val url  = routes.Clas.show(clas.id.value).url
                  val full = if (text contains url) text else s"$text\n\n${env.net.baseUrl}$url"
                  env.msg.api.multiPost(me, Source(students.map(_.user.id)), full) inject
                    Redirect(routes.Clas.show(clas.id.value)).flashSuccess
                }
              }
          )
      }
    }

  def students(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.api.student.allWithUsers(clas) flatMap { students =>
          env.clas.api.invite.listPending(clas) map { invites =>
            views.html.clas.teacherDashboard.students(clas, students, invites)
          }
        }
      }
    }

  def progress(id: String, key: String, days: Int) =
    Secure(_.Teacher) { implicit ctx => me =>
      lila.rating.PerfType(key) ?? { perfType =>
        WithClass(me, id) { clas =>
          env.clas.api.student.activeWithUsers(clas) flatMap { students =>
            Reasonable(clas, students, "progress") {
              env.clas.progressApi(perfType, days, students) map { progress =>
                views.html.clas.teacherDashboard.progress(clas, students, progress)
              }
            }
          }
        }
      }
    }

  def learn(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.api.student.activeWithUsers(clas) flatMap { students =>
          Reasonable(clas, students, "progress") {
            val studentIds = students.map(_.user.id)
            env.learn.api.completionPercent(studentIds) zip
              env.practice.api.progress.completionPercent(studentIds) zip
              env.coordinate.api.bestScores(studentIds) map { case basic ~ practice ~ coords =>
                views.html.clas.teacherDashboard.learn(clas, students, basic, practice, coords)
              }
          }
        }
      }
    }

  def edit(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.api.student.activeWithUsers(clas) map { students =>
          Ok(html.clas.clas.edit(clas, students, env.clas.forms.clas.edit(clas)))
        }
      }
    }

  def update(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClass(me, id) { clas =>
        env.clas.forms.clas
          .edit(clas)
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err =>
              env.clas.api.student.activeWithUsers(clas) map { students =>
                BadRequest(html.clas.clas.edit(clas, students, err))
              },
            data =>
              env.clas.api.clas.update(clas, data) map { clas =>
                Redirect(routes.Clas.show(clas.id.value)).flashSuccess
              }
          )
      }
    }

  def archive(id: String, v: Boolean) =
    SecureBody(_.Teacher) { _ => me =>
      WithClass(me, id) { clas =>
        env.clas.api.clas.archive(clas, me, v) inject
          Redirect(routes.Clas.show(clas.id.value)).flashSuccess
      }
    }

  def studentForm(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      if (getBool("gen")) env.clas.nameGenerator() map {
        Ok(_)
      }
      else
        WithClassAndStudents(me, id) { (clas, students) =>
          for {
            created <- ctx.req.flash.get("created").map(_ split ' ').?? {
              case Array(userId, password) =>
                env.clas.api.student
                  .get(clas, userId)
                  .map2(lila.clas.Student.WithPassword(_, lila.user.User.ClearPassword(password)))
              case _ => fuccess(none)
            }
            nbStudents <- env.clas.api.student.count(clas.id)
            createForm <- env.clas.forms.student.generate
          } yield Ok(
            html.clas.student.form(
              clas,
              students,
              env.clas.forms.student.invite(clas),
              createForm,
              nbStudents,
              created
            )
          )
        }
    }

  def studentCreate(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      NoTor {
        Firewall {
          SafeTeacher {
            WithClassAndStudents(me, id) { (clas, students) =>
              env.clas.forms.student.create
                .bindFromRequest()(ctx.body, formBinding)
                .fold(
                  err =>
                    env.clas.api.student.count(clas.id) map { nbStudents =>
                      BadRequest(
                        html.clas.student.form(
                          clas,
                          students,
                          env.clas.forms.student.invite(clas),
                          err,
                          nbStudents
                        )
                      )
                    },
                  data =>
                    env.clas.api.student.create(clas, data, me) map { s =>
                      Redirect(routes.Clas.studentForm(clas.id.value))
                        .flashing("created" -> s"${s.student.userId} ${s.password.value}")
                    }
                )
            }
          }
        }
      }
    }

  def studentManyForm(id: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        ctx.req.flash.get("created").?? {
          _.split('/').toList
            .flatMap {
              _.split(' ') match {
                case Array(u, p) => (u, p).some
                case _           => none
              }
            }
            .map { case (u, p) =>
              env.clas.api.student
                .get(clas, u)
                .map2(lila.clas.Student.WithPassword(_, lila.user.User.ClearPassword(p)))
            }
            .sequenceFu
            .map(_.flatten)
        } flatMap { created =>
          env.clas.api.student.count(clas.id) map { nbStudents =>
            val form = env.clas.forms.student.manyCreate(lila.clas.Clas.maxStudents - nbStudents)
            Ok(html.clas.student.manyForm(clas, students, form, nbStudents, created))
          }
        }
      }
    }

  def studentManyCreate(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      NoTor {
        Firewall {
          SafeTeacher {
            WithClassAndStudents(me, id) { (clas, students) =>
              env.clas.api.student.count(clas.id) flatMap { nbStudents =>
                env.clas.forms.student
                  .manyCreate(lila.clas.Clas.maxStudents - nbStudents)
                  .bindFromRequest()(ctx.body, formBinding)
                  .fold(
                    err => BadRequest(html.clas.student.manyForm(clas, students, err, nbStudents)).fuccess,
                    data =>
                      env.clas.api.student.manyCreate(clas, data, me) flatMap { many =>
                        env.user.lightUserApi.preloadMany(many.map(_.student.userId)) inject
                          Redirect(routes.Clas.studentManyForm(clas.id.value))
                            .flashing(
                              "created" -> many
                                .map { s =>
                                  s"${s.student.userId} ${s.password.value}"
                                }
                                .mkString("/")
                            )
                      }
                  )
              }
            }
          }
        }
      }
    }

  def studentInvite(id: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        env.clas.forms.student
          .invite(clas)
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err =>
              env.clas.api.student.count(clas.id) map { nbStudents =>
                BadRequest(
                  html.clas.student.form(
                    clas,
                    students,
                    err,
                    env.clas.forms.student.create,
                    nbStudents
                  )
                )
              },
            data =>
              env.user.repo named data.username flatMap {
                _ ?? { user =>
                  import lila.clas.ClasInvite.{ Feedback => F }
                  env.clas.api.invite.create(clas, user, data.realName, me) map { feedback =>
                    Redirect(routes.Clas.studentForm(clas.id.value)).flashing {
                      feedback match {
                        case F.Already => "success" -> s"${user.username} is now a student of the class"
                        case F.Invited => "success" -> s"An invitation has been sent to ${user.username}"
                        case F.Found   => "warning" -> s"${user.username} already has a pending invitation"
                        case F.CantMsgKid(url) =>
                          "warning" -> s"${user.username} is a kid account and can't receive your message. You must give them the invitation URL manually: $url"
                      }
                    }
                  }
                }
              }
          )
      }
    }

  def studentShow(id: String, username: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        WithStudent(clas, username) { s =>
          env.activity.read.recent(s.user, 14) map { activity =>
            views.html.clas.student.show(clas, students, s, activity)
          }
        }
      }
    }

  def studentEdit(id: String, username: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        WithStudent(clas, username) { s =>
          Ok(views.html.clas.student.edit(clas, students, s, env.clas.forms.student edit s.student)).fuccess
        }
      }
    }

  def studentUpdate(id: String, username: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        WithStudent(clas, username) { s =>
          env.clas.forms.student
            .edit(s.student)
            .bindFromRequest()(ctx.body, formBinding)
            .fold(
              err => BadRequest(html.clas.student.edit(clas, students, s, err)).fuccess,
              data =>
                env.clas.api.student.update(s.student, data) map { _ =>
                  Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).flashSuccess
                }
            )
        }
      }
    }

  def studentArchive(id: String, username: String, v: Boolean) =
    Secure(_.Teacher) { _ => me =>
      WithClass(me, id) { clas =>
        WithStudent(clas, username) { s =>
          env.clas.api.student.archive(s.student.id, me, v) inject
            Redirect(routes.Clas.studentShow(clas.id.value, username)).flashSuccess
        }
      }
    }

  def studentResetPassword(id: String, username: String) =
    Secure(_.Teacher) { _ => me =>
      WithClass(me, id) { clas =>
        WithStudent(clas, username) { s =>
          env.security.store.closeAllSessionsOf(s.user.id) >>
            env.clas.api.student.resetPassword(s.student) map { password =>
              Redirect(routes.Clas.studentShow(clas.id.value, username))
                .flashing("password" -> password.value)
            }
        }
      }
    }

  def studentRelease(id: String, username: String) =
    Secure(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        WithStudent(clas, username) { s =>
          if (s.student.managed)
            Ok(views.html.clas.student.release(clas, students, s, env.clas.forms.student.release)).fuccess
          else
            Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).fuccess
        }
      }
    }

  def studentReleasePost(id: String, username: String) =
    SecureBody(_.Teacher) { implicit ctx => me =>
      WithClassAndStudents(me, id) { (clas, students) =>
        WithStudent(clas, username) { s =>
          if (s.student.managed)
            env.security.forms.preloadEmailDns(ctx.body, formBinding) >> env.clas.forms.student.release
              .bindFromRequest()(ctx.body, formBinding)
              .fold(
                err => BadRequest(html.clas.student.release(clas, students, s, err)).fuccess,
                data => {
                  val email = env.security.emailAddressValidator
                    .validate(lila.common.EmailAddress(data)) err s"Invalid email $data"
                  val newUserEmail = lila.security.EmailConfirm.UserEmail(s.user.username, email.acceptable)
                  authC.EmailConfirmRateLimit(newUserEmail, ctx.req) {
                    env.security.emailChange.send(s.user, newUserEmail.email) inject
                      Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).flashSuccess {
                        s"A confirmation email was sent to ${email.acceptable.value}. ${s.student.realName} must click the link in the email to release the account."
                      }
                  }(rateLimitedFu)
                }
              )
          else
            Redirect(routes.Clas.studentShow(clas.id.value, s.user.username)).fuccess
        }
      }
    }

  def becomeTeacher =
    AuthBody { _ => me =>
      val perm = lila.security.Permission.Teacher.dbKey
      (!me.roles.has(perm) ?? env.user.repo.setRoles(me.id, perm :: me.roles).void) inject
        Redirect(routes.Clas.index)
    }

  def invitation(id: String) =
    Auth { implicit ctx => me =>
      OptionOk(env.clas.api.invite.view(lila.clas.ClasInvite.Id(id), me)) { case (invite -> clas) =>
        views.html.clas.invite.show(clas, invite)
      }
    }

  def invitationAccept(id: String) =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      Form(single("v" -> boolean))
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.Clas.invitation(id)).fuccess,
          v => {
            val inviteId = lila.clas.ClasInvite.Id(id)
            if (v) env.clas.api.invite.accept(inviteId, me) map {
              _ ?? { student =>
                Redirect(routes.Clas.show(student.clasId.value))
              }
            }
            else
              env.clas.api.invite.decline(inviteId) inject
                Redirect(routes.Clas.invitation(id))
          }
        )
    }

  def invitationRevoke(id: String) =
    Secure(_.Teacher) { _ => me =>
      env.clas.api.invite.get(lila.clas.ClasInvite.Id(id)) flatMap {
        _ ?? { invite =>
          WithClass(me, invite.clasId.value) { clas =>
            env.clas.api.invite.delete(invite._id) inject Redirect(routes.Clas.students(clas.id.value))
          }
        }
      }
    }

  private def Reasonable(clas: lila.clas.Clas, students: List[lila.clas.Student.WithUser], active: String)(
      f: => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    if (students.sizeIs <= lila.clas.Clas.maxStudents) f
    else Unauthorized(views.html.clas.teacherDashboard.unreasonable(clas, students, active)).fuccess

  private def WithClass(me: lila.user.User, clasId: String)(
      f: lila.clas.Clas => Fu[Result]
  ): Fu[Result] =
    env.clas.api.clas.getAndView(lila.clas.Clas.Id(clasId), me) flatMap { _ ?? f }

  private def WithClassAndStudents(me: lila.user.User, clasId: String)(
      f: (lila.clas.Clas, List[lila.clas.Student]) => Fu[Result]
  ): Fu[Result] =
    WithClass(me, clasId) { c =>
      env.clas.api.student.activeOf(c) flatMap { f(c, _) }
    }

  private def WithStudent(clas: lila.clas.Clas, username: String)(
      f: lila.clas.Student.WithUser => Fu[Result]
  ): Fu[Result] =
    env.user.repo named username flatMap {
      _ ?? { user =>
        env.clas.api.student.get(clas, user) flatMap { _ ?? f }
      }
    }

  private def SafeTeacher(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.exists(!_.lameOrTroll)) f
    else Redirect(routes.Clas.index).fuccess
}
