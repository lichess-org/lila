package controllers

import play.api.mvc._

import lila.app._
import views._

final class Clas(
    env: Env,
    prismicC: Prismic
) extends LilaController(env) {

  def index = Open { implicit ctx =>
    ctx.me.ifTrue(isGranted(_.Teacher)).ifFalse(getBool("home")).map { me =>
      WithTeacher(me) { t =>
        env.clas.api.clas.of(t.teacher) map { classes =>
          Ok(views.html.clas.clas.index(classes))
        }
      }
    } | {
      pageHit
      prismicC getBookmark "class" map {
        _ ?? {
          case (doc, resolver) => Ok(views.html.clas.clas.home(doc, resolver))
        }
      }
    }
  }

  def form = Secure(_.Teacher) { implicit ctx => _ =>
    Ok(html.clas.clas.create(env.clas.forms.create)).fuccess
  }

  def create = SecureBody(_.Teacher) { implicit ctx => me =>
    WithTeacher(me) { t =>
      env.clas.forms.create
        .bindFromRequest()(ctx.body)
        .fold(
          err => BadRequest(html.clas.clas.create(err)).fuccess,
          data =>
            env.clas.api.clas.create(data, t.teacher) map { clas =>
              Redirect(routes.Clas.show(clas.id.value))
            }
        )
    }
  }

  private def preloadStudentUsers(students: List[lila.clas.Student.WithUser]): Unit =
    env.user.lightUserApi.preloadUsers(students.map(_.user))

  def show(id: String) = Auth { implicit ctx => me =>
    isGranted(_.Teacher).??(env.clas.api.clas.isTeacherOf(me, lila.clas.Clas.Id(id))) flatMap {
      case true =>
        WithClass(me, id) { _ => clas =>
          env.clas.api.student.activeWithUsers(clas) map { students =>
            preloadStudentUsers(students)
            views.html.clas.teacherDashboard.active(clas, students)
          }
        }
      case _ =>
        env.clas.api.clas.byId(lila.clas.Clas.Id(id)) flatMap {
          _ ?? { clas =>
            env.clas.api.student.activeWithUsers(clas) flatMap { students =>
              if (students.exists(_.student is me)) {
                preloadStudentUsers(students)
                Ok(views.html.clas.studentDashboard(clas, students)).fuccess
              } else notFound
            }
          }
        }
    }
  }

  def archived(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { _ => clas =>
      env.clas.api.student.allWithUsers(clas) map { students =>
        views.html.clas.teacherDashboard.archived(clas, students)
      }
    }
  }

  def perfType(id: String, key: String) = Secure(_.Teacher) { implicit ctx => me =>
    lila.rating.PerfType(key) ?? { perfType =>
      WithClass(me, id) { _ => clas =>
        env.clas.api.student.activeWithUsers(clas) map { students =>
          views.html.clas.teacherDashboard.perf(clas, students, perfType)
        }
      }
    }
  }

  def edit(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { _ => clas =>
      Ok(html.clas.clas.edit(clas, env.clas.forms.edit(clas))).fuccess
    }
  }

  def update(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { _ => clas =>
      env.clas.forms
        .edit(clas)
        .bindFromRequest()(ctx.body)
        .fold(
          err => BadRequest(html.clas.clas.edit(clas, err)).fuccess,
          data =>
            env.clas.api.clas.update(clas, data) map { clas =>
              Redirect(routes.Clas.show(clas.id.value)).flashSuccess
            }
        )
    }
  }

  def archive(id: String, v: Boolean) = SecureBody(_.Teacher) { _ => me =>
    WithClass(me, id) { t => clas =>
      env.clas.api.clas.archive(clas, t.teacher, v) inject
        Redirect(routes.Clas.show(clas.id.value)).flashSuccess
    }
  }

  def studentForm(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    if (getBool("gen")) env.clas.nameGenerator() map {
      Ok(_)
    } else
      WithClassAndStudents(me, id) { _ => (clas, students) =>
        ctx.req.flash.get("created").map(_ split ' ').?? {
          case Array(userId, password) =>
            env.clas.api.student
              .get(clas, userId)
              .map2(lila.clas.Student.WithPassword(_, lila.user.User.ClearPassword(password)))
          case _ => fuccess(none)
        } flatMap { created =>
          env.clas.forms.student.generate map { createForm =>
            Ok(
              html.clas.student.form(
                clas,
                students,
                env.clas.forms.student.invite,
                createForm,
                created
              )
            )
          }
        }
      }
  }

  def studentCreate(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    NoTor {
      Firewall {
        WithClassAndStudents(me, id) { t => (clas, students) =>
          env.clas.forms.student.create
            .bindFromRequest()(ctx.body)
            .fold(
              err =>
                BadRequest(
                  html.clas.student.form(
                    clas,
                    students,
                    env.clas.forms.student.invite,
                    err
                  )
                ).fuccess,
              data =>
                env.clas.api.student.create(clas, data, t) map {
                  case (user, password) =>
                    Redirect(routes.Clas.studentForm(clas.id.value))
                      .flashing("created" -> s"${user.id} ${password.value}")
                }
            )
        }
      }
    }
  }

  def studentInvite(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClassAndStudents(me, id) { t => (clas, students) =>
      env.clas.forms.student.invite
        .bindFromRequest()(ctx.body)
        .fold(
          err =>
            BadRequest(
              html.clas.student.form(
                clas,
                students,
                err,
                env.clas.forms.student.create
              )
            ).fuccess,
          data =>
            env.user.repo named data.username flatMap {
              _ ?? { user =>
                env.clas.api.student.invite(clas, user, data.realName, t) map { so =>
                  Redirect(routes.Clas.studentForm(clas.id.value)).flashing {
                    so.fold("warning" -> s"${user.username} is already in the class") { s =>
                      "success" -> s"${user.username} (${s.realName}) has been invited"
                    }
                  }
                }
              }
            }
        )
    }
  }

  def studentShow(id: String, username: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClassAndStudents(me, id) { _ => (clas, students) =>
      WithStudent(clas, username) { s =>
        env.activity.read.recent(s.user, 14) map { activity =>
          views.html.clas.student.show(clas, students, s, activity)
        }
      }
    }
  }

  def studentEdit(id: String, username: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClassAndStudents(me, id) { _ => (clas, students) =>
      WithStudent(clas, username) { s =>
        Ok(views.html.clas.student.edit(clas, students, s, env.clas.forms.student edit s.student)).fuccess
      }
    }
  }

  def studentUpdate(id: String, username: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClassAndStudents(me, id) { _ => (clas, students) =>
      WithStudent(clas, username) { s =>
        env.clas.forms.student
          .edit(s.student)
          .bindFromRequest()(ctx.body)
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

  def studentArchive(id: String, username: String, v: Boolean) = Secure(_.Teacher) { _ => me =>
    WithClass(me, id) { t => clas =>
      WithStudent(clas, username) { s =>
        env.clas.api.student.archive(s.student, t.teacher, v) inject
          Redirect(routes.Clas.studentShow(clas.id.value, username)).flashSuccess
      }
    }
  }

  def studentSetKid(id: String, username: String, v: Boolean) = Secure(_.Teacher) { _ => me =>
    WithClass(me, id) { _ => clas =>
      WithStudent(clas, username) { s =>
        (s.student.managed ?? env.user.repo.setKid(s.user, v)) inject
          Redirect(routes.Clas.studentShow(clas.id.value, username)).flashSuccess
      }
    }
  }

  def studentResetPassword(id: String, username: String) = Secure(_.Teacher) { _ => me =>
    WithClass(me, id) { _ => clas =>
      WithStudent(clas, username) { s =>
        env.clas.api.student.resetPassword(s.student) map { password =>
          Redirect(routes.Clas.studentShow(clas.id.value, username))
            .flashing("password" -> password.value)
        }
      }
    }
  }

  private def WithTeacher(me: lila.user.User)(
      f: lila.clas.Teacher.WithUser => Fu[Result]
  ): Fu[Result] =
    env.clas.api.teacher withOrCreate me flatMap f

  private def WithClass(me: lila.user.User, clasId: String)(
      f: lila.clas.Teacher.WithUser => lila.clas.Clas => Fu[Result]
  ): Fu[Result] =
    WithTeacher(me) { t =>
      env.clas.api.clas.getAndView(lila.clas.Clas.Id(clasId), t.teacher) flatMap {
        _ ?? f(t)
      }
    }

  private def WithClassAndStudents(me: lila.user.User, clasId: String)(
      f: lila.clas.Teacher.WithUser => (lila.clas.Clas, List[lila.clas.Student]) => Fu[Result]
  ): Fu[Result] =
    WithClass(me, clasId) { t => c =>
      env.clas.api.student.activeOf(c) flatMap { students =>
        f(t)(c, students)
      }
    }

  private def WithStudent(clas: lila.clas.Clas, username: String)(
      f: lila.clas.Student.WithUser => Fu[Result]
  ): Fu[Result] =
    env.user.repo named username flatMap {
      _ ?? { user =>
        env.clas.api.student.get(clas, user) flatMap {
          _ ?? f
        }
      }
    }
}
