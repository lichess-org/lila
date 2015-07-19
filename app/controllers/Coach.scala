package controllers

import lila.api.Context
import lila.app._
import play.api.mvc.Result
import views._

object Coach extends LilaController {

  private def env = Env.coach

  def raw(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.statApi.fetch(user.id) map { stat =>
        Ok(html.coach.raw.index(user, stat))
      }
    }
  }

  def json(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.statApi.fetchOrCompute(user.id) flatMap env.jsonView.apply map { json =>
        Ok(json) as JSON
      }
    }
  }

  def opening(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.statApi.fetch(user.id) map { stat =>
        Ok(html.coach.opening(user, stat))
      }
    }
  }

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.statApi.computeIfOld(user.id) inject
        Redirect(routes.Coach.raw(user.username))
    }
  }

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      case None => notFound
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true  => f(u)
        case false => fuccess(Forbidden(html.coach.forbidden(u)))
      }
    }
}
