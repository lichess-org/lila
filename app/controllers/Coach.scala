package controllers

import lila.app._
import lila.user.UserRepo
import views._

object Coach extends LilaController {

  private def env = Env.coach

  def raw(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.statApi.fetch(user.id) map { stat =>
        html.coach.raw.index(user, stat)
      }
    }
  }

  def json(username: String) = Open { implicit ctx =>
    JsonOptionFuOk(UserRepo named username) { user =>
      env.statApi.fetchOrCompute(user.id) flatMap env.jsonView.apply
    }
  }

  def opening(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.statApi.fetch(user.id) map { stat =>
        html.coach.opening(user, stat)
      }
    }
  }

  def refresh(username: String) = Open { implicit ctx =>
    OptionFuRedirect(UserRepo named username) { user =>
      env.statApi.computeIfOld(user.id) inject routes.Coach.raw(user.username)
    }
  }
}
