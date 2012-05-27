package controllers

import lila._
import views._
import http.Context

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object Message extends LilaController {

  def api = env.message.api
  def forms = env.message.forms

  def inbox(page: Int) = Auth { implicit ctx ⇒
    implicit me ⇒
      Ok(html.message.inbox(api.inbox(me, page)))
  }

  def thread(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionOk(api.thread(id, me)) { html.message.thread(_) }
  }

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      Ok(html.message.form(forms.thread))
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      implicit val req = ctx.body
      forms.thread.bindFromRequest.fold(
        err ⇒ BadRequest(html.message.form(err)),
        data ⇒ api.makeThread(data, me).map(thread ⇒
          Redirect(routes.Message.thread(thread.id))
        ).unsafePerformIO
      )
  }

  def delete(id: String) = TODO
}
