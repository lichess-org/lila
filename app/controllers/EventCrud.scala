package controllers

import play.api.mvc._

import lila.app._
import views._

object EventCrud extends LilaController {

  private def env = Env.event
  private def api = env.api

  def index = Secure(_.ManageEvent) { implicit ctx =>
    me =>
      api.list map { events =>
        html.event.index(events)
      }
  }

  def edit(id: String) = Secure(_.ManageEvent) { implicit ctx =>
    me =>
      OptionOk(api one id) { event =>
        html.event.edit(event, api editForm event)
      }
  }

  def update(id: String) = SecureBody(_.ManageEvent) { implicit ctx =>
    me =>
      OptionFuResult(api one id) { event =>
        implicit val req = ctx.body
        api.editForm(event).bindFromRequest.fold(
          err => BadRequest(html.event.edit(event, err)).fuccess,
          data => api.update(event, data) inject Redirect(routes.EventCrud.edit(id))
        )
      }
  }

  def form = Secure(_.ManageEvent) { implicit ctx =>
    me =>
      Ok(html.event.create(api.createForm)).fuccess
  }

  def create = SecureBody(_.ManageEvent) { implicit ctx =>
    me =>
      implicit val req = ctx.body
      api.createForm.bindFromRequest.fold(
        err => BadRequest(html.event.create(err)).fuccess,
        data => api.create(data, me.id) map { event =>
          Redirect(routes.EventCrud.edit(event.id))
        }
      )
  }
}
