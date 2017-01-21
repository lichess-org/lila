package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import views._

object Practice extends LilaController {

  private def env = Env.practice

  def index = Open { implicit ctx =>
    env.api.get(ctx.me) map { p =>
      Ok(html.practice.index(p))
    }
  }

  def config = Auth { implicit ctx => me =>
    for {
      struct <- env.api.structure.get
      form <- env.api.config.form
    } yield Ok(html.practice.config(struct, form))
  }

  def configSave = SecureBody(_.StreamConfig) { implicit ctx => me =>
    implicit val req = ctx.body
    env.api.config.form.flatMap { form =>
      FormFuResult(form) { err =>
        env.api.structure.get map { html.practice.config(_, err) }
      } { text =>
        env.api.config.set(text).valueOr(_ => funit) >>
          env.api.structure.clear >>
          Env.mod.logApi.practiceConfig(me.id) inject Redirect(routes.Practice.config)
      }
    }
  }
}
