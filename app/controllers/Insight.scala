package controllers

import play.api.i18n.Lang
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.insight.{ InsightDimension, InsightMetric }

final class Insight(env: Env) extends LilaController(env):

  def refresh(username: UserStr) =
    OpenOrScoped()(
      open = implicit ctx =>
        Accessible(username) { user =>
          env.insight.api indexAll user inject Ok
        },
      scoped = req =>
        me =>
          AccessibleApi(username)(me.some) { user =>
            env.insight.api indexAll user inject Ok
          }
    )

  def index(username: UserStr) =
    def jsonStatus(user: lila.user.User) =
      env.insight.api userStatus user map { status =>
        Ok(Json.obj("status" -> status.toString))
      }
    OpenOrScoped()(
      open = implicit ctx =>
        Accessible(username) { user =>
          render.async {
            case Accepts.Html() => doPath(user, InsightMetric.MeanCpl.key, InsightDimension.Perf.key, "")
            case Accepts.Json() => jsonStatus(user)
          }
        },
      scoped = req => me => AccessibleApi(username)(me.some)(jsonStatus)
    )

  def path(username: UserStr, metric: String, dimension: String, filters: String) =
    Open { implicit ctx =>
      Accessible(username) { doPath(_, metric, dimension, ~lila.common.String.decodeUriPath(filters)) }
    }

  private def doPath(user: lila.user.User, metric: String, dimension: String, filters: String)(using
      ctx: Context
  ) =
    import lila.insight.InsightApi.UserStatus.*
    env.insight.api userStatus user flatMap {
      case NoGame => Ok(html.site.message.insightNoGames(user)).toFuccess
      case Empty  => Ok(html.insight.empty(user)).toFuccess
      case s =>
        for {
          insightUser <- env.insight.api insightUser user
          prefId      <- env.insight.share getPrefId user
        } yield Ok(
          html.insight.index(
            u = user,
            insightUser = insightUser,
            prefId = prefId,
            ui = env.insight.jsonView
              .ui(insightUser.families, insightUser.openings, asMod = isGranted(_.ViewBlurs)),
            question = env.insight.jsonView.question(metric, dimension, filters),
            stale = s == Stale
          )
        )
    }

  def json(username: UserStr) =
    OpenOrScopedBody(parse.json)(Nil)(
      open = implicit ctx => {
        AccessibleApi(username)(ctx.me) { user =>
          processQuestion(user, ctx.body)
        }
      },
      scoped = req =>
        me =>
          AccessibleApi(username)(me.some) { user =>
            processQuestion(user, req)(reqLang(req))
          }
    )

  private def processQuestion(user: lila.user.User, body: Request[JsValue])(implicit lang: Lang) =
    import lila.insight.JsonQuestion, JsonQuestion.*
    body.body
      .validate[JsonQuestion]
      .fold(
        err => BadRequest(jsonError(err.toString)).toFuccess,
        _.question.fold(BadRequest.toFuccess) { q =>
          env.insight.api.ask(q, user) flatMap
            lila.insight.Chart.fromAnswer(env.user.lightUser) map
            env.insight.jsonView.chartWrites.writes map { Ok(_) }
        }
      )

  private def Accessible(username: UserStr)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    env.user.repo byId username flatMap {
      _.fold(notFound) { u =>
        env.insight.share.grant(u, ctx.me) flatMap {
          case true => f(u)
          case _    => fuccess(Forbidden(html.insight.forbidden(u)))
        }
      }
    }

  private def AccessibleApi(username: UserStr)(me: Option[lila.user.User])(f: lila.user.User => Fu[Result]) =
    env.user.repo byId username flatMap {
      _ ?? { u =>
        env.insight.share.grant(u, me) flatMap {
          case true => f(u)
          case _    => fuccess(Forbidden)
        }
      }
    }
