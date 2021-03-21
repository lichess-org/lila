package controllers

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.insight.{ Dimension, Metric }

final class Insight(env: Env) extends LilaController(env) {

  def refresh(username: String) =
    OpenOrScoped()(
      open = implicit ctx =>
        Accessible(username) { user =>
          env.insight.api indexAll user.id inject Ok
        },
      scoped = req =>
        me =>
          AccessibleApi(username)(me.some) { user =>
            env.insight.api indexAll user.id inject Ok
          }
    )

  def index(username: String) = {
    def jsonStatus(user: lila.user.User) =
      env.insight.api userStatus user map { status =>
        Ok(Json.obj("status" -> status.toString))
      }
    OpenOrScoped()(
      open = implicit ctx =>
        Accessible(username) { user =>
          render.async {
            case Accepts.Html() => doPath(user, Metric.MeanCpl.key, Dimension.Perf.key, "")
            case Accepts.Json() => jsonStatus(user)
          }
        },
      scoped = req => me => AccessibleApi(username)(me.some)(jsonStatus)
    )
  }

  def path(username: String, metric: String, dimension: String, filters: String) =
    Open { implicit ctx =>
      Accessible(username) { doPath(_, metric, dimension, filters) }
    }

  private def doPath(user: lila.user.User, metric: String, dimension: String, filters: String)(implicit
      ctx: Context
  ) = {
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

  def json(username: String) =
    OpenOrScopedBody(parse.json)(Nil)(
      open = implicit ctx => {
        AccessibleApi(username)(ctx.me) { user =>
          processQuestion(user, ctx.body)
        }
      },
      scoped = req =>
        me =>
          AccessibleApi(username)(me.some) { user =>
            processQuestion(user, req)
          }
    )

  private def processQuestion(user: lila.user.User, body: Request[JsValue]) = {
    import lila.insight.JsonQuestion, JsonQuestion._
    implicit val lang = reqLang(body)
    body.body
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

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    env.user.repo named username flatMap {
      _.fold(notFound) { u =>
        env.insight.share.grant(u, ctx.me) flatMap {
          case true => f(u)
          case _    => fuccess(Forbidden(html.insight.forbidden(u)))
        }
      }
    }

  private def AccessibleApi(username: String)(me: Option[lila.user.User])(f: lila.user.User => Fu[Result]) =
    env.user.repo named username flatMap {
      _ ?? { u =>
        env.insight.share.grant(u, me) flatMap {
          case true => f(u)
          case _    => fuccess(Forbidden)
        }
      }
    }
}
