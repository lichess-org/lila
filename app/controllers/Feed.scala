package controllers

import play.api.mvc.*
import views.*
import java.time.LocalDate

import lila.app.{ given, * }
import lila.common.config.Max
import lila.feed.Feed.Update

final class Feed(env: Env) extends LilaController(env):

  def api = env.feed.api

  def index(page: Int) = Open: ctx ?=>
    Reasonable(page):
      for
        updates      <- env.feed.paginator.recent(isGrantedOpt(_.Feed), page)
        renderedPage <- renderPage(html.feed.index(updates))
      yield Ok(renderedPage)

  def createForm = Secure(_.Feed) { _ ?=> _ ?=>
    Ok.pageAsync(html.feed.create(api.form(none)))
  }

  def create = SecureBody(_.Feed) { _ ?=> _ ?=>
    api
      .form(none)
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(html.feed.create(err)),
        data =>
          val up = data toUpdate none
          api.set(up) inject Redirect(routes.Feed.edit(up.id)).flashSuccess
      )
  }

  def edit(id: String) = Secure(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      Ok.pageAsync(html.feed.edit(api.form(up.some), up))
  }

  def update(id: String) = SecureBody(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): from =>
      api
        .form(from.some)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(html.feed.edit(err, from)),
          data => api.set(data toUpdate from.id.some) inject Redirect(routes.Feed.edit(from.id)).flashSuccess
        )
  }

  def delete(id: String) = Secure(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id) inject Redirect(routes.Feed.index(1)).flashSuccess
  }

  def atom = Anon:
    api.recentPublished map: ups =>
      Ok(html.feed.atom(ups)) as XML
