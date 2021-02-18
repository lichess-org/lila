package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._

final class Storm(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      NoBot {
        env.storm.selector.apply flatMap { puzzles =>
          ctx.userId.?? { u => env.storm.highApi.get(u) dmap some } map { high =>
            NoCache {
              Ok(
                views.html.storm.home(
                  env.storm.json(puzzles, ctx.me),
                  env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE)),
                  high
                )
              )
            }
          }
        }
      }
    }

  def record =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        env.storm.forms.run
          .bindFromRequest()
          .fold(
            _ => fuccess(none),
            data => env.storm.dayApi.addRun(data, ctx.me)
          ) map env.storm.json.newHigh map { json =>
          Ok(json) as JSON
        }
      }
    }

  def dashboard(page: Int) =
    Auth { implicit ctx => me =>
      renderDashboardOf(me, page)
    }

  def dashboardOf(username: String, page: Int) =
    Open { implicit ctx =>
      env.user.repo.enabledNamed(username).flatMap {
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

  def apiDashboardOf(username: String, days: Int) =
    Open { implicit ctx =>
      val userId = lila.user.User normalize username
      if (days < 0 || days > 365) notFoundJson("Invalid days parameter")
      else
        ((days > 0) ?? env.storm.dayApi.apiHistory(userId, days)) zip env.storm.highApi.get(userId) map {
          case (history, high) =>
            Ok(env.storm.json.apiDashboard(high, history))
        }

    }
}
