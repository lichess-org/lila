package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser.lightUserWrites

final class Msg(
    env: Env
) extends LilaController(env) {

  def home = Auth { implicit ctx => me =>
    env.msg.api.threads(me) flatMap env.msg.json.threads(me) map { threads =>
      Ok(
        views.html.msg.home(
          Json.obj(
            "me"      -> me.light,
            "threads" -> threads
          )
        )
      )
    }
  }
}
