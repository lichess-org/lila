package controllers

import lila.api.Context
import lila.app._
import play.api.libs.json.Json
import play.api.mvc._
import views._

object Coach extends LilaController {

  private def env = Env.coach

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.indexer(user) inject Ok
    }
  }

  def index(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      import lila.coach.CoachApi.UserStatus._
      env.api.userStatus(user).map {
        case NoGame => Ok(html.coach.noGame(user))
        case Empty  => Ok(html.coach.empty(user))
        case s      => Ok(html.coach.index(user, env.jsonView.stringifiedUi, s == Stale))
      }
    }
  }

  def json(username: String) = OpenBody(BodyParsers.parse.json) { implicit ctx =>
    import lila.coach.JsonQuestion, JsonQuestion._
    Accessible(username) { user =>
      ctx.body.body.validate[JsonQuestion].fold(
        err => BadRequest(Json.obj("error" -> err.toString)).fuccess,
        qJson => qJson.question.fold(BadRequest.fuccess) { q =>
          env.api.ask(q, user) map
            lila.coach.Chart.fromAnswer map
            env.jsonView.chart.apply map { Ok(_) }
        }
      )
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

  private def AccessibleJson(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      case None => notFoundJson(s"No such user: $username")
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true                          => f(u)
        case false if isGranted(_.UserSpy) => f(u)
        case false                         => fuccess(Forbidden(Json.obj("error" -> s"User $username data is protected")))
      }
    } map (_ as JSON)
}
