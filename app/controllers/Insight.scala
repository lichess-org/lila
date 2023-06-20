package controllers

import play.api.i18n.Lang
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.insight.{ InsightDimension, InsightMetric }

final class Insight(env: Env) extends LilaController(env):

  def refresh(username: UserStr) = OpenOrScoped(): ctx ?=>
    AccessibleApi(username): user =>
      env.insight.api indexAll user inject Ok

  def index(username: UserStr) =
    def jsonStatus(user: lila.user.User) =
      env.insight.api userStatus user map { status =>
        Ok(Json.obj("status" -> status.toString))
      }
    OpenOrScoped()(
      open = ctx ?=>
        Accessible(username): user =>
          render.async:
            case Accepts.Html() => doPath(user, InsightMetric.MeanCpl.key, InsightDimension.Perf.key, "")
            case Accepts.Json() => jsonStatus(user)
      ,
      scoped = ctx ?=> AccessibleApi(username)(jsonStatus)
    )

  def path(username: UserStr, metric: String, dimension: String, filters: String) = Open:
    Accessible(username) { doPath(_, metric, dimension, ~lila.common.String.decodeUriPath(filters)) }

  private def doPath(user: lila.user.User, metric: String, dimension: String, filters: String)(using
      ctx: WebContext
  ) =
    import lila.insight.InsightApi.UserStatus.*
    env.insight.api userStatus user flatMap {
      case NoGame => Ok(html.site.message.insightNoGames(user))
      case Empty  => Ok(html.insight.empty(user))
      case s =>
        for
          insightUser <- env.insight.api insightUser user
          prefId      <- env.insight.share getPrefId user
        yield Ok(
          html.insight.index(
            u = user,
            insightUser = insightUser,
            prefId = prefId,
            ui = env.insight.jsonView
              .ui(insightUser.families, insightUser.openings, asMod = isGrantedOpt(_.ViewBlurs)),
            question = env.insight.jsonView.question(metric, dimension, filters),
            stale = s == Stale
          )
        )
    }

  def json(username: UserStr) =
    import lila.app.ui.EmbedConfig.given
    OpenOrScopedBody(parse.json)(Nil): ctx ?=>
      AccessibleApi(username) { processQuestion(_, ctx.body) }

  private def processQuestion(user: lila.user.User, body: Request[JsValue])(using Lang) =
    body.body
      .validate[lila.insight.JsonQuestion]
      .fold(
        err => BadRequest(jsonError(err.toString)).toFuccess,
        _.question.fold(BadRequest.toFuccess): q =>
          env.insight.api.ask(q, user) flatMap
            lila.insight.Chart.fromAnswer(env.user.lightUser) map
            env.insight.jsonView.chartWrites.writes map { Ok(_) }
      )

  private def Accessible(username: UserStr)(f: lila.user.User => Fu[Result])(using ctx: WebContext) =
    env.user.repo byId username flatMap {
      _.fold(notFound): u =>
        env.insight.share.grant(u) flatMap {
          if _ then f(u)
          else Forbidden(html.insight.forbidden(u))
        }
    }

  private def AccessibleApi(
      username: UserStr
  )(f: lila.user.User => Fu[Result])(using me: Option[Me]) =
    env.user.repo byId username flatMapz { u =>
      env.insight.share.grant(u) flatMap {
        if _ then f(u)
        else Forbidden
      }
    }
