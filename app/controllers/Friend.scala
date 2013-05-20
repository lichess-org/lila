package controllers

import lila.app._
import lila.user.{ User ⇒ UserModel, Context }
import views._

import play.api.mvc._
import play.api.templates.Html

object Friend extends LilaController {

  private def env = Env.friend

  def yes(friendId: String) = Auth { implicit ctx ⇒
    me ⇒
      env.api.yes(me.id, friendId) map { status ⇒
        html.friend.status(me, status)
      }
  }

  def no(friendId: String) = TODO

}
