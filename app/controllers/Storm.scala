package controllers

import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }

final class Storm(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env):

  def home     = Open(serveHome(_))
  def homeLang = LangPage(routes.Storm.home)(serveHome(_))
  private def serveHome(implicit ctx: Context) = NoBot {
    env.storm.selector.apply flatMap { puzzles =>
      ctx.userId.?? { u => env.storm.highApi.get(u) dmap some } map { high =>
        Ok(
          views.html.storm.home(
            env.storm.json(puzzles, ctx.me),
            env.storm.json.pref(ctx.pref),
            high
          )
        ).noCache
      }
    }
  }

  def record =
    OpenBody { implicit ctx =>
      NoBot {
        given play.api.mvc.Request[?] = ctx.body
        env.storm.forms.run
          .bindFromRequest()
          .fold(
            _ => fuccess(none),
            data => env.storm.dayApi.addRun(data, ctx.me)
          ) map env.storm.json.newHigh map JsonOk
      }
    }

  def dashboard(page: Int) =
    Auth { implicit ctx => me =>
      renderDashboardOf(me, page)
    }

  def dashboardOf(username: UserStr, page: Int) =
    Open { implicit ctx =>
      env.user.repo.enabledById(username).flatMap {
        _ ?? {
          renderDashboardOf(_, page)
        }
      }
    }

  private def renderDashboardOf(user: lila.user.User, page: Int)(implicit ctx: Context): Fu[Result] =
    env.storm.dayApi.history(user.id, page) flatMap { history =>
      env.storm.highApi.get(user.id) map { high =>
        Ok(views.html.storm.dashboard(user, history, high))
      }
    }

  def apiDashboardOf(username: UserStr, days: Int) =
    Open { implicit ctx =>
      lila.user.User.validateId(username) ?? { userId =>
        if (days < 0 || days > 365) notFoundJson("Invalid days parameter")
        else
          ((days > 0) ?? env.storm.dayApi.apiHistory(userId, days)) zip env.storm.highApi.get(userId) map {
            case (history, high) =>
              Ok(env.storm.json.apiDashboard(high, history))
          }
      }
    }
