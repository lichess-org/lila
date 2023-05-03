package controllers

import lila.app.{ given, * }
import views.*

final class Event(env: Env) extends LilaController(env):

  private def api = env.event.api

  def show(id: String) = Open:
    OptionOk(api oneEnabled id): event =>
      html.event.show(event)

  def manager =
    Secure(_.ManageEvent) { implicit ctx => _ =>
      api.list map { events =>
        html.event.manager(events)
      }
    }

  def edit(id: String) =
    Secure(_.ManageEvent) { implicit ctx => _ =>
      OptionOk(api one id) { event =>
        html.event.edit(event, api editForm event)
      }
    }

  def update(id: String) =
    SecureBody(_.ManageEvent) { implicit ctx => me =>
      OptionFuResult(api one id) { event =>
        given play.api.mvc.Request[?] = ctx.body
        api
          .editForm(event)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.event.edit(event, err)).toFuccess,
            data => api.update(event, data, me.user) inject Redirect(routes.Event.edit(id)).flashSuccess
          )
      }
    }

  def form =
    Secure(_.ManageEvent) { implicit ctx => _ =>
      Ok(html.event.create(api.createForm)).toFuccess
    }

  def create =
    SecureBody(_.ManageEvent) { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
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

  def cloneE(id: String) =
    Secure(_.ManageEvent) { implicit ctx => _ =>
      OptionFuResult(api one id) { old =>
        val event = api clone old
        Ok(html.event.create(api editForm event)).toFuccess
      }
    }
