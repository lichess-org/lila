package controllers

import lila.api.Context
import lila.app._
import lila.insight.{ Dimension, Metric }
import play.api.mvc._
import views._

final class Insight(env: Env) extends LilaController(env) {

  def refresh(username: String) =
    Open { implicit ctx =>
      Accessible(username) { user =>
        env.insight.api indexAll user.id inject Ok
      }
    }

  def index(username: String) =
    path(
      username,
      metric = Metric.MeanCpl.key,
      dimension = Dimension.Perf.key,
      filters = ""
    )

  def path(username: String, metric: String, dimension: String, filters: String) =
    Open { implicit ctx =>
      Accessible(username) { user =>
        import lila.insight.InsightApi.UserStatus._
        env.insight.api userStatus user flatMap {
          case NoGame => Ok(html.site.message.insightNoGames(user)).fuccess
          case Empty  => Ok(html.insight.empty(user)).fuccess
          case s =>
            for {
              cache  <- env.insight.api insightUser user
              prefId <- env.insight.share getPrefId user
            } yield Ok(
              html.insight.index(
                u = user,
                cache = cache,
                prefId = prefId,
                ui = env.insight.jsonView.ui(cache.ecos, asMod = isGranted(_.ViewBlurs)),
                question = env.insight.jsonView.question(metric, dimension, filters),
                stale = s == Stale
              )
            )
        }
      }
    }

  def json(username: String) =
    OpenBody(parse.json) { implicit ctx =>
      import lila.insight.JsonQuestion, JsonQuestion._
      Accessible(username) { user =>
        ctx.body.body
          .validate[JsonQuestion]
          .fold(
            err => BadRequest(jsonError(err.toString)).fuccess,
            _.question.fold(BadRequest.fuccess) { q =>
              env.insight.api.ask(q, user) map
                lila.insight.Chart.fromAnswer(env.user.lightUserSync) map
                env.insight.jsonView.chart.apply map { Ok(_) }
            }
          )
      }
    }

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    env.user.repo named username flatMap {
      _.fold(notFound) { u =>
        env.insight.share.grant(u, ctx.me) flatMap {
          case true => f(u)
          case _    => fuccess(Forbidden(html.insight.forbidden(u)))
        }
      }
    }
}
