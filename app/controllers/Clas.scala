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
        Ok(views.html.clas.clas.index(classes, t))
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

  def show(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, lila.clas.Clas.Id(id)) { t => clas =>
      env.clas.api.student.of(clas) map { students =>
        views.html.clas.clas.show(clas, t, students)
      }
    }
  }

  def edit(id: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, lila.clas.Clas.Id(id)) { _ => clas =>
      Ok(html.clas.clas.edit(clas, env.clas.forms.edit(clas))).fuccess
    }
  }

  def update(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClass(me, lila.clas.Clas.Id(id)) { _ => clas =>
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
    WithClass(me, lila.clas.Clas.Id(id)) { _ => clas =>
      Ok(html.clas.student.form(clas, env.clas.forms.student.create)).fuccess
    }
  }

  def studentCreate(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    NoTor {
      Firewall {
        WithClass(me, lila.clas.Clas.Id(id)) { _ => clas =>
          env.clas.forms.student.create
            .bindFromRequest()(ctx.body)
            .fold(
              err => BadRequest(html.clas.student.form(clas, err)).fuccess,
              username =>
                env.clas.api.student.create(clas, username)(env.user.authenticator.passEnc) flatMap {
                  case (user, password) =>
                    env.clas.api.student.get(clas, user) map {
                      _ ?? { student =>
                        Ok(html.clas.student.show(clas, student, password.some))
                      }
                    }
                }
            )
        }
      }
    }
  }

  def studentShow(id: String, username: String) = Secure(_.Teacher) { implicit ctx => me =>
    WithClass(me, lila.clas.Clas.Id(id)) { t => clas =>
      env.user.repo named username flatMap {
        _ ?? { user =>
          env.clas.api.student.get(clas, user) map {
            _ ?? { student =>
              views.html.clas.student.show(clas, student)
            }
          }
        }
      }
    }
  }

  private def WithTeacher(me: lila.user.User)(
      f: lila.clas.Teacher.WithUser => Fu[Result]
  ): Fu[Result] =
    env.clas.api.teacher withOrCreate me flatMap f

  private def WithClass(me: lila.user.User, clasId: lila.clas.Clas.Id)(
      f: lila.clas.Teacher.WithUser => lila.clas.Clas => Fu[Result]
  ): Fu[Result] =
    WithTeacher(me) { t =>
      env.clas.api.clas.getAndView(clasId, t.teacher) flatMap {
        _ ?? f(t)
      }
    }
}
