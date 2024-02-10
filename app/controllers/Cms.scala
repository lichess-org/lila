package controllers

import play.api.mvc.*
import views.*
import lila.app.{ given, * }
import lila.cms.CmsPage

final class Cms(env: Env) extends LilaController(env):

  def api = env.cms.api

  def index = Secure(_.Pages): ctx ?=>
    for
      pages        <- api.list
      renderedPage <- renderPage(html.cms.index(pages))
    yield Ok(renderedPage)

  def createForm = Secure(_.Pages) { _ ?=> _ ?=>
    Ok.pageAsync(html.cms.create(env.cms.form.create))
  }

  def create = SecureBody(_.Pages) { _ ?=> me ?=>
    env.cms.form.create
      .bindFromRequest()
      .fold(
        err => BadRequest.pageAsync(html.cms.create(err)),
        data =>
          val page = data create me
          api.create(page) inject Redirect(routes.Cms.edit(page.id)).flashSuccess
      )
  }

  def edit(id: CmsPage.Id) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.withAlternatives(id)): pages =>
      Ok.pageAsync(html.cms.edit(env.cms.form.edit(pages.head), pages.head, pages.tail))
  }

  def update(id: CmsPage.Id) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(api.withAlternatives(id)): pages =>
      env.cms.form
        .edit(pages.head)
        .bindFromRequest()
        .fold(
          err => BadRequest.pageAsync(html.cms.edit(err, pages.head, pages.tail)),
          data =>
            api.update(pages.head, data) map: page =>
              Redirect(routes.Cms.edit(page.id)).flashSuccess
        )
  }

  def delete(id: CmsPage.Id) = Secure(_.Pages) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      api.delete(up.id) inject Redirect(routes.Cms.index).flashSuccess
  }
