package controllers

import play.api.mvc.*
import views.*
import java.time.LocalDate

import lila.app.{ given, * }
import lila.common.config.Max
import lila.blog.DailyFeed.Update

final class DailyFeed(env: Env) extends LilaController(env):

  def api       = env.blog.dailyFeed
  def paginator = env.blog.dailyFeedPaginator

  def index(page: Int) = Open: ctx ?=>
    Reasonable(page):
      for
        updates      <- paginator.recent(isGrantedOpt(_.DailyFeed), page)
        renderedPage <- renderPage(html.dailyFeed.index(updates))
      yield Ok(renderedPage)

  def createForm = Secure(_.DailyFeed) { _ ?=> _ ?=>
    Ok.pageAsync(html.dailyFeed.create(api.form(none)))
  }

  def create = SecureBody(_.DailyFeed) { _ ?=> _ ?=>
    api
      .form(none)
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(html.dailyFeed.create(err)),
        data =>
          val up = data toUpdate none
          api.set(up) inject Redirect(routes.DailyFeed.edit(up.id)).flashSuccess
      )
  }

  def edit(id: String) = Secure(_.DailyFeed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      Ok.pageAsync(html.dailyFeed.edit(api.form(up.some), up))
  }

  def update(id: String) = SecureBody(_.DailyFeed) { _ ?=> _ ?=>
    Found(api.get(id)): from =>
      api
        .form(from.some)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(html.dailyFeed.edit(err, from)),
          data =>
            api.set(data toUpdate from.id.some) inject Redirect(routes.DailyFeed.edit(from.id)).flashSuccess
        )
  }

  def delete(id: String) = Secure(_.DailyFeed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id) inject Redirect(routes.DailyFeed.index(1)).flashSuccess
  }

  def atom = Anon:
    api.recentPublished map: ups =>
      Ok(html.dailyFeed.atom(ups)) as XML
