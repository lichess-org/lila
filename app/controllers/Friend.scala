package controllers

import lila.app._
import lila.user.{ User ⇒ UserModel, Context }
import views._

import play.api.mvc._
import play.api.templates.Html

object Friend extends LilaController {

  private def env = Env.friend

  def yes(friendId: String) = Open { implicit ctx ⇒
    ctx.userId.fold(Ok(html.friend.button(friendId)).fuccess) { userId ⇒
      env.api.yes(userId, userId).nevermind inject html.friend.button(friendId)
    }
  }

  def no(friendId: String) = Auth { implicit ctx ⇒
    me ⇒
      env.api.no(me.id, friendId).nevermind inject html.friend.button(friendId)
  }
}
