package controllers

import lila.app._
import views._

final class Appeal(env: Env) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
      env.appeal.api.mine(me) map { appeal =>
        Ok(html.appeal2.home(appeal, env.appeal.forms.text))
      }
    }

  def post =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      env.appeal.forms.text
        .bindFromRequest()
        .fold(
          err =>
            env.appeal.api.mine(me) map { appeal =>
              BadRequest(html.appeal2.home(appeal, err))
            },
          text =>
            env.appeal.api.post(text, me) inject Redirect(routes.Appeal.home()).flashSuccess
        )
    }
}
