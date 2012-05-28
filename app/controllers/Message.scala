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
      IOptionOk(api.thread(id, me)) { thread ⇒
        html.message.thread(thread, forms.post)
      }
  }

  def answer(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IOptionIOResult(api.thread(id, me)) { thread ⇒
        implicit val req = ctx.body
        forms.post.bindFromRequest.fold(
          err ⇒ io {
            BadRequest(html.message.thread(thread, err) + "#bottom")
          },
          text ⇒ api.makePost(thread, text, me).map(post ⇒
            Redirect(routes.Message.thread(thread.id) + "#bottom")
          ))
      }
  }

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      Ok(html.message.form(forms.thread))
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IOResult {
        implicit val req = ctx.body
        forms.thread.bindFromRequest.fold(
          err ⇒ io(BadRequest(html.message.form(err))),
          data ⇒ api.makeThread(data, me).map(thread ⇒
            Redirect(routes.Message.thread(thread.id))
          ))
      }
  }

  def delete(id: String) = TODO
}
