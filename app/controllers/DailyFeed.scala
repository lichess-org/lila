package controllers

import play.api.mvc.*
import views.*
import java.time.LocalDate

import lila.app.{ given, * }
import lila.common.config.Max
import lila.blog.DailyFeed.Update

final class DailyFeed(env: Env) extends LilaController(env):

  def api = env.blog.dailyFeed

  def index = Open:
    for
      updates <- api.recent(Max(50))
      page    <- renderPage(html.dailyFeed.index(updates))
    yield Ok(page)

  private def get(day: String): Fu[Option[Update]] =
    scala.util.Try(LocalDate.parse(day)).toOption.so(api.get)

  def createForm = Secure(_.Prismic) { ctx ?=> me ?=>
    Ok.pageAsync(html.dailyFeed.create(api.form(none)))
  }

  def create = SecureBody(_.Prismic) { ctx ?=> me ?=>
    api
      .form(none)
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(html.dailyFeed.create(err)),
        up => api.set(up, none) inject Redirect(routes.DailyFeed.edit(up.day)).flashSuccess
      )
  }

  def edit(day: String) = Secure(_.Prismic) { ctx ?=> me ?=>
    Found(get(day)): up =>
      Ok.pageAsync(html.dailyFeed.edit(api.form(up.some), up))
  }

  def update(day: String) = SecureBody(_.Prismic) { ctx ?=> me ?=>
    Found(get(day)): from =>
      api
        .form(from.some)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(html.dailyFeed.edit(err, from)),
          up => api.set(up, from.some) inject Redirect(routes.DailyFeed.edit(up.day)).flashSuccess
        )
  }

  def delete(day: String) = Secure(_.Prismic) { ctx ?=> me ?=>
    Found(get(day)): up =>
      api.delete(up.day) inject Redirect(routes.DailyFeed.index).flashSuccess
  }
