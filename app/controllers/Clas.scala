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
    Ok(html.clas.form.create(env.clas.forms.create)).fuccess
  }

  def create = SecureBody(_.Teacher) { implicit ctx => me =>
    WithTeacher(me) { t =>
      env.clas.forms.create
        .bindFromRequest()(ctx.body)
        .fold(
          err => BadRequest(html.clas.form.create(err)).fuccess,
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
      Ok(html.clas.form.edit(clas, env.clas.forms.edit(clas))).fuccess
    }
  }

  def update(id: String) = SecureBody(_.Teacher) { implicit ctx => me =>
    WithClass(me, lila.clas.Clas.Id(id)) { _ => clas =>
      env.clas.forms
        .edit(clas)
        .bindFromRequest()(ctx.body)
        .fold(
          err => BadRequest(html.clas.form.edit(clas, err)).fuccess,
          setup =>
            env.clas.api.clas.update(clas, setup) map { clas =>
              Redirect(routes.Clas.show(clas.id.value))
            }
        )
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
