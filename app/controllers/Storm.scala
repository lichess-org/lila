package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest

final class Storm(env: Env) extends LilaController(env):

  def home = Open(serveHome)
  def homeLang = LangPage(routes.Storm.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    dataAndHighScore(ctx.pref.some).flatMap: (data, high) =>
      Ok.page(views.storm.home(data, high)).map(_.noCache)

  private def dataAndHighScore(pref: Option[lila.pref.Pref])(using me: Option[Me]) = for
    puzzles <- env.storm.selector.apply
    high <- me.soFu(m => env.storm.highApi.get(m.userId))
  yield env.storm.json(puzzles, me, pref) -> high

  def apiGet = AnonOrScoped(_.Puzzle.Read, _.Web.Mobile): ctx ?=>
    dataAndHighScore(none).map: (data, high) =>
      import lila.storm.StormJson.given
      JsonOk(data.add("high" -> high))

  def record = OpenOrScopedBody(parse.anyContent)(_.Puzzle.Write, _.Web.Mobile): ctx ?=>
    NoBot:
      bindForm(env.storm.forms.run)(
        _ => fuccess(none),
        data => env.storm.dayApi.addRun(data, ctx.me, mobile = HTTPRequest.isLichessMobile(req))
      )
        .map(env.storm.json.newHigh)
        .map(JsonOk)

  def dashboard(page: Int) = Auth { ctx ?=> me ?=>
    renderDashboardOf(me, page)
  }

  def dashboardOf(username: UserStr, page: Int) = Open:
    Found(env.user.repo.enabledById(username)):
      renderDashboardOf(_, page)

  private def renderDashboardOf(user: lila.user.User, page: Int)(using Context): Fu[Result] = for
    history <- env.storm.dayApi.history(user.id, page)
    high <- env.storm.highApi.get(user.id)
    page <- renderPage(views.storm.dashboard(user, history, high))
  yield Ok(page)

  def apiDashboardOf(username: UserStr, days: Int) = Open:
    username.validateId.so: userId =>
      if days < 0 || days > 365 then notFoundJson("Invalid days parameter")
      else
        for
          history <- (days > 0).so(env.storm.dayApi.apiHistory(userId, days))
          high <- env.storm.highApi.get(userId)
        yield Ok(env.storm.json.apiDashboard(high, history))
