package controllers

import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.HTTPRequest

final class Storm(env: Env) extends LilaController(env):

  def home     = Open(serveHome(using _))
  def homeLang = LangPage(routes.Storm.home)(serveHome(using _))
  private def serveHome(using ctx: Context) = NoBot {
    dataAndHighScore(ctx.me, ctx.pref.some) map { (data, high) =>
      Ok(views.html.storm.home(data, high)).noCache
    }
  }

  private def dataAndHighScore(me: Option[lila.user.User], pref: Option[lila.pref.Pref]) =
    env.storm.selector.apply flatMap { puzzles =>
      me.?? { u => env.storm.highApi.get(u.id) dmap some } map { high =>
        env.storm.json(puzzles, me, pref) -> high
      }
    }

  def apiGet = AnonOrScoped(_.Puzzle.Read) { _ => me =>
    dataAndHighScore(me, none) map { (data, high) =>
      import lila.storm.StormJson.given
      JsonOk(data.add("high" -> high))
    }
  }

  def record =
    def doRecord(me: Option[lila.user.User], mobile: Boolean)(using Request[?]) =
      env.storm.forms.run
        .bindFromRequest()
        .fold(
          _ => fuccess(none),
          data => env.storm.dayApi.addRun(data, me, mobile = mobile)
        ) map env.storm.json.newHigh map JsonOk
    OpenOrScopedBody(parse.anyContent)(Seq(_.Puzzle.Write))(
      open = ctx => NoBot { doRecord(ctx.me, mobile = false)(using ctx.body) }(using ctx),
      scoped = req => me => doRecord(me.some, mobile = HTTPRequest.isLichessMobile(req))(using req)
    )

  def dashboard(page: Int) =
    Auth { implicit ctx => me =>
      renderDashboardOf(me, page)
    }

  def dashboardOf(username: UserStr, page: Int) =
    Open { implicit ctx =>
      env.user.repo.enabledById(username).flatMapz {
        renderDashboardOf(_, page)
      }
    }

  private def renderDashboardOf(user: lila.user.User, page: Int)(implicit ctx: Context): Fu[Result] =
    env.storm.dayApi.history(user.id, page) flatMap { history =>
      env.storm.highApi.get(user.id) map { high =>
        Ok(views.html.storm.dashboard(user, history, high))
      }
    }

  def apiDashboardOf(username: UserStr, days: Int) =
    Open { _ =>
      lila.user.User.validateId(username) ?? { userId =>
        if (days < 0 || days > 365) notFoundJson("Invalid days parameter")
        else
          ((days > 0) ?? env.storm.dayApi.apiHistory(userId, days)) zip env.storm.highApi.get(userId) map {
            case (history, high) =>
              Ok(env.storm.json.apiDashboard(high, history))
          }
      }
    }
