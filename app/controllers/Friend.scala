package controllers

import lila.app._
import lila.user.{ User ⇒ UserModel, Context }
import views._

import play.api.mvc._
import play.api.templates.Html

object Friend extends LilaController {

  private def env = Env.friend

  def add(userId: String) = Auth { implicit ctx ⇒
    me ⇒
      env.api.createRequest(userId, me.id) map { html.friend.status(_) }
  }

}
