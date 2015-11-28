package controllers

import lila.api.Context
import lila.app._
import lila.insight.{ Metric, Dimension }
import play.api.libs.json.Json
import play.api.mvc._
import views._

object Insight extends LilaController {

  private def env = Env.insight

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.api indexAll user inject Ok
    }
  }

  def index(username: String) = path(username,
    metric = Metric.MeanCpl.key,
    dimension = Dimension.PieceRole.key,
    filters = "")

  def path(username: String, metric: String, dimension: String, filters: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      import lila.insight.InsightApi.UserStatus._
      env.api userStatus user flatMap {
        case NoGame => Ok(html.insight.noGame(user)).fuccess
        case Empty  => Ok(html.insight.empty(user)).fuccess
        case s => env.api count user map { nbGames =>
          Ok(html.insight.index(
            u = user,
            nbGames = nbGames,
            ui = env.jsonView.stringifiedUi,
            question = env.jsonView.question(metric, dimension, filters),
            stale = s == Stale))
        }
      }
    }
  }

  def json(username: String) = OpenBody(BodyParsers.parse.json) { implicit ctx =>
    import lila.insight.JsonQuestion, JsonQuestion._
    Accessible(username) { user =>
      ctx.body.body.validate[JsonQuestion].fold(
        err => BadRequest(Json.obj("error" -> err.toString)).fuccess,
        qJson => qJson.question.fold(BadRequest.fuccess) { q =>
          env.api.ask(q, user) map
            lila.insight.Chart.fromAnswer map
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
        case false                         => fuccess(Forbidden(html.insight.forbidden(u)))
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
