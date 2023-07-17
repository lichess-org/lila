package controllers

import lila.app.{ given, * }
import views.*

final class Event(env: Env) extends LilaController(env):

  private def api = env.event.api

  def show(id: String) = Open:
    FoundPage(api oneEnabled id)(html.event.show)

  def manager = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    Ok.pageAsync:
      api.list map html.event.manager
  }

  def edit(id: String) = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    FoundPage(api one id): event =>
      html.event.edit(event, api editForm event)
  }

  def update(id: String) = SecureBody(_.ManageEvent) { ctx ?=> me ?=>
    Found(api one id): event =>
      api
        .editForm(event)
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.event.edit(event, err)),
          data => api.update(event, data) inject Redirect(routes.Event.edit(id)).flashSuccess
        )
  }

  def form = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    Ok.page:
      html.event.create(api.createForm)
  }

  def create = SecureBody(_.ManageEvent) { ctx ?=> me ?=>
    api.createForm
      .bindFromRequest()
      .fold(
        err => BadRequest.page(html.event.create(err)),
        data =>
          api.create(data) map { event =>
            Redirect(routes.Event.edit(event.id)).flashSuccess
          }
      )
  }

  def cloneE(id: String) = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    FoundPage(api one id): old =>
      html.event.create(api editForm api.clone(old))
  }
