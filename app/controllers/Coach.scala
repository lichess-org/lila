package controllers

import lila.app._
import lila.user.UserRepo
import views._

object Coach extends LilaController {

  private def env = Env.coach

  def stat(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.statApi.fetch(user.id) map { stat =>
        html.coach.stat(user, stat)
      }
    }
  }

  def refresh(username: String) = Open { implicit ctx =>
    OptionFuRedirect(UserRepo named username) { user =>
      env.statApi.computeIfOld(user.id) inject routes.Coach.stat(user.username)
    }
  }
}
