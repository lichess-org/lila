package controllers

import lila.api.Context
import lila.app._
import play.api.libs.json.Json
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
    lila.user.UserRepo named username flatMap {
      case None => fuccess(NotFound(Json.obj("error" -> s"User $username not found")))
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true  => env.statApi.fetchOrCompute(u.id) flatMap env.jsonView.raw map { Ok(_) }
        case false => fuccess(Forbidden(Json.obj("error" -> s"User $username data is protected")))
      }
    } map (_ as JSON)
  }

  def opening(username: String, colorStr: String) = Open { implicit ctx =>
    chess.Color(colorStr).fold(notFound) { color =>
      Accessible(username) { user =>
        env.statApi.fetch(user.id) flatMap { stat =>
          stat ?? { s =>
            env.jsonView.opening(s, color).map(json => (s, json).some)
          } map { data =>
            Ok(html.coach.opening(user, color, data))
          }
        }
      }
    }
  }

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      {
        if (isGranted(_.SuperAdmin)) env.statApi.computeForce(user.id)
        else env.statApi.computeIfOld(user.id)
      } inject Ok
    }
  }

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      case None => notFound
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true                          => f(u)
        case false if isGranted(_.UserSpy) => f(u)
        case false                         => fuccess(Forbidden(html.coach.forbidden(u)))
      }
    }
}
