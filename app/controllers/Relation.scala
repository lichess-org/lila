package controllers

import lila.app._
import lila.user.{ User ⇒ UserModel, UserRepo, Context }
import views._

import play.api.mvc._
import play.api.templates.Html

object Relation extends LilaController {

  private def env = Env.relation

  def follow(userId: String) = Open { implicit ctx ⇒
    ctx.userId.fold(Ok(html.relation.actions(userId, true)).fuccess) { myId ⇒
      env.api.follow(myId, userId).nevermind inject html.relation.actions(userId)
    }
  }

  def unfollow(userId: String) = Auth { implicit ctx ⇒
    me ⇒
      env.api.unfollow(me.id, userId).nevermind inject html.relation.actions(userId)
  }

  def block(userId: String) = Open { implicit ctx ⇒
    ctx.userId.fold(Ok(html.relation.actions(userId, true)).fuccess) { myId ⇒
      env.api.block(myId, userId).nevermind inject html.relation.actions(userId)
    }
  }

  def unblock(userId: String) = Auth { implicit ctx ⇒
    me ⇒
      env.api.unblock(me.id, userId).nevermind inject html.relation.actions(userId)
  }

  def following(userId: String) = Open { implicit ctx ⇒
    OptionFuOk(UserRepo named userId) { user ⇒
      env.api.following(user.id) map { userIds ⇒
        html.relation.following(user, userIds.toList.sorted)
      }
    }
  }

  def followers(userId: String) = Open { implicit ctx ⇒
    OptionFuOk(UserRepo named userId) { user ⇒
      env.api.followers(user.id) map { userIds ⇒
        html.relation.followers(user, userIds.toList.sorted)
      }
    }
  }
}
