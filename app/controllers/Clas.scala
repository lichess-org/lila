package controllers

import play.api.mvc._

import lila.app._
import views._

final class Clas(
    env: Env
) extends LilaController(env) {

  def index = Secure(_.Teacher) { implicit ctx => me =>
    WithTeacher(me) { t =>
      env.clas.api.clas.of(t.teacher) map { classes =>
        Ok(views.html.clas.clas.index(classes))
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
          setup =>
            env.clas.api.clas.create(setup, t.teacher) map { clas =>
              Redirect(routes.Clas.show(clas.id.value))
            }
        )
    }
  }

  def show(id: String) = Auth { implicit ctx => me =>
    isGranted(_.Teacher).??(env.clas.api.clas.isTeacherOf(me, lila.clas.Clas.Id(id))) flatMap {
      case true =>
        WithClass(me, id) { _ => clas =>
          env.clas.api.student.allOf(clas) map { students =>
            views.html.clas.clas.showToTeacher(clas, students)
          }
        }
      case _ =>
        env.clas.api.clas.byId(lila.clas.Clas.Id(id)) flatMap {
          _ ?? { clas =>
            env.clas.api.student.activeOf(clas) flatMap { students =>
              if (students.exists(_.student is me))
                Ok(views.html.clas.clas.showToStudent(clas, students)).fuccess
              else notFound
            }
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
          setup =>
            env.clas.api.clas.update(clas, setup) map { clas =>
              Redirect(routes.Clas.show(clas.id.value))
            }
        )
    }
  }

  def studentForm(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { _ => clas =>
      Ok(
        html.clas.student.form(
          clas,
          env.clas.forms.student.invite,
          env.clas.forms.student.create
        )
      ).fuccess
    }
  }

  def studentCreate(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    NoTor {
      Firewall {
        WithClass(me, id) { t => clas =>
          env.clas.forms.student.create
            .bindFromRequest()(ctx.body)
            .fold(
              err =>
                BadRequest(
                  html.clas.student.form(
                    clas,
                    env.clas.forms.student.invite,
                    err
                  )
                ).fuccess,
              username =>
                env.clas.api.student.create(clas, username, t) map {
                  case (user, password) =>
                    Redirect(routes.Clas.studentShow(clas.id.value, user.username))
                      .flashing("password" -> password.value)
                }
            )
        }
      }
    }
  }

  def studentInvite(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { t => clas =>
      env.clas.forms.student.invite
        .bindFromRequest()(ctx.body)
        .fold(
          err =>
            BadRequest(
              html.clas.student.form(
                clas,
                err,
                env.clas.forms.student.create
              )
            ).fuccess,
          username =>
            env.user.repo named username flatMap {
              _ ?? { user =>
                env.clas.api.student.invite(clas, user, t) inject
                  Redirect(routes.Clas.studentForm(clas.id.value)).flashSuccess
              }
            }
        )
    }
  }

  def studentJoin(id: String, token: String) = Auth { _ => me =>
    env.clas.api.invite.redeem(lila.clas.Clas.Id(id), me, token) map {
      _ ?? { _ =>
        Redirect(routes.Clas.show(id))
      }
    }
  }

  def studentShow(id: String, username: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, id) { _ => clas =>
      env.user.repo named username flatMap {
        _ ?? { user =>
          env.clas.api.student.get(clas, user) flatMap {
            _ ?? { student =>
              env.activity.read.recent(student.user, 14) map { activity =>
                views.html.clas.student.show(clas, student, activity)
              }
            }
          }
        }
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
