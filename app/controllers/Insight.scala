package controllers

import lila.api.Context
import lila.app._
import lila.insight.{ Metric, Dimension }
import play.api.mvc._
import views._

object Insight extends LilaController {

  private def env = Env.insight

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.api indexAll user inject Ok
    }
  }

  def index(username: String) = path(
    username,
    metric = Metric.MeanCpl.key,
    dimension = Dimension.Perf.key,
    filters = ""
  )

  def path(username: String, metric: String, dimension: String, filters: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      import lila.insight.InsightApi.UserStatus._
      env.api userStatus user flatMap {
        case NoGame => Ok(html.site.message.insightNoGames(user)).fuccess
        case Empty => Ok(html.insight.empty(user)).fuccess
        case s => for {
          cache <- env.api userCache user
          prefId <- env.share getPrefId user
        } yield Ok(html.insight.index(
          u = user,
          cache = cache,
          prefId = prefId,
          ui = env.jsonView.ui(cache.ecos),
          question = env.jsonView.question(metric, dimension, filters),
          stale = s == Stale
        ))
      }
    }
  }

  def json(username: String) = OpenBody(BodyParsers.parse.json) { implicit ctx =>
    import lila.insight.JsonQuestion, JsonQuestion._
    Accessible(username) { user =>
      ctx.body.body.validate[JsonQuestion].fold(
        err => BadRequest(jsonError(err.toString)).fuccess,
        qJson => qJson.question.fold(BadRequest.fuccess) { q =>
          env.api.ask(q, user) map
            lila.insight.Chart.fromAnswer(Env.user.lightUserSync) map
            env.jsonView.chart.apply map { Ok(_) }
        }
      )
    }
  }

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      _.fold(notFound) { u =>
        env.share.grant(u, ctx.me) flatMap {
          case true => f(u)
          case _ => fuccess(Forbidden(html.insight.forbidden(u)))
        }
      }
    }
}
