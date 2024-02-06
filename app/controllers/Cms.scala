package controllers

import play.api.mvc.*
import views.*
import lila.app.{ given, * }
import lila.cms.CmsPage

final class Cms(env: Env) extends LilaController(env):

  def api = env.cms.api

  def index = Secure(_.Prismic): ctx ?=>
    for
      pages        <- api.list
      renderedPage <- renderPage(html.cms.index(pages))
    yield Ok(renderedPage)

  def createForm = Secure(_.Prismic) { _ ?=> _ ?=>
    Ok.pageAsync(html.cms.create(env.cms.form.create))
  }

  def create = SecureBody(_.Prismic) { _ ?=> me ?=>
    env.cms.form.create
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(html.cms.create(err)),
        data =>
          val page = data create me
          api.create(page) inject Redirect(routes.Cms.edit(page.id)).flashSuccess
      )
  }

  def edit(id: CmsPage.Id) = Secure(_.Prismic) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      Ok.pageAsync(html.cms.edit(env.cms.form.edit(up), up))
  }

  def update(id: CmsPage.Id) = SecureBody(_.Prismic) { _ ?=> me ?=>
    Found(api.get(id)): from =>
      env.cms.form
        .edit(from)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(html.cms.edit(err, from)),
          data =>
            api.update(from, data) map: page =>
              Redirect(routes.Cms.edit(page.id)).flashSuccess
        )
  }

  def delete(id: CmsPage.Id) = Secure(_.Prismic) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id) inject Redirect(routes.Cms.index).flashSuccess
  }
