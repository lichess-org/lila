package controllers

import lila.app._
import views._

final class Event(env: Env) extends LilaController(env) {

  private def api = env.event.api

  def show(id: String) =
    Open { implicit ctx =>
      OptionOk(api oneEnabled id) { event =>
        html.event.show(event)
      }
    }

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
        implicit val req = ctx.body
        api
          .editForm(event)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.event.edit(event, err)).fuccess,
            data => api.update(event, data, me.user) inject Redirect(routes.Event.edit(id)).flashSuccess
          )
      }
    }

  def form =
    Secure(_.ManageEvent) { implicit ctx => _ =>
      Ok(html.event.create(api.createForm)).fuccess
    }

  def create =
    SecureBody(_.ManageEvent) { implicit ctx => me =>
      implicit val req = ctx.body
      api.createForm
        .bindFromRequest()
        .fold(
          err => BadRequest(html.event.create(err)).fuccess,
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
        Ok(html.event.create(api editForm event)).fuccess
      }
    }
}
