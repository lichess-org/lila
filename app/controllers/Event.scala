package controllers

import lila.app.{ given, * }
import views.*

final class Event(env: Env) extends LilaController(env):

  private def api = env.event.api

  def show(id: String) = Open:
    OptionOk(api oneEnabled id):
      html.event.show

  def manager = Secure(_.ManageEvent) { ctx ?=> _ =>
    api.list map html.event.manager
  }

  def edit(id: String) = Secure(_.ManageEvent) { ctx ?=> _ =>
    OptionOk(api one id): event =>
      html.event.edit(event, api editForm event)
  }

  def update(id: String) = SecureBody(_.ManageEvent) { ctx ?=> me =>
    OptionFuResult(api one id): event =>
      api
        .editForm(event)
        .bindFromRequest()
        .fold(
          err => BadRequest(html.event.edit(event, err)).toFuccess,
          data => api.update(event, data, me.user) inject Redirect(routes.Event.edit(id)).flashSuccess
        )
  }

  def form = Secure(_.ManageEvent) { ctx ?=> _ =>
    Ok(html.event.create(api.createForm)).toFuccess
  }

  def create = SecureBody(_.ManageEvent) { ctx ?=> me =>
    api.createForm
      .bindFromRequest()
      .fold(
        err => BadRequest(html.event.create(err)).toFuccess,
        data =>
          api.create(data, me.id) map { event =>
            Redirect(routes.Event.edit(event.id)).flashSuccess
          }
      )
  }

  def cloneE(id: String) = Secure(_.ManageEvent) { ctx ?=> _ =>
    OptionFuResult(api one id): old =>
      val event = api clone old
      Ok(html.event.create(api editForm event)).toFuccess
  }
