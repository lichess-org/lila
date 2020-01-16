package controllers

import play.api.mvc._

import lila.api.Context
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
    WithTeacher(me) { t =>
      env.clas.api.clas.getAndView(lila.clas.Clas.Id(id), t.teacher) map {
        _ ?? { clas =>
          views.html.clas.clas.show(clas, t)
        }
      }
    }
  }

  private def WithTeacher(me: lila.user.User)(
      f: lila.clas.Teacher.WithUser => Fu[Result]
  ): Fu[Result] =
    env.clas.api.teacher withOrCreate me flatMap f
}
