package controllers

import lila._
import views._
import http.Context
import user.{ User ⇒ UserModel }

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
      IfCanMessage {
        IOptionIOResult(api.thread(id, me)) { thread ⇒
          implicit val req = ctx.body
          forms.post.bindFromRequest.fold(
            err ⇒ io {
              BadRequest(html.message.thread(thread, err))
            },
            text ⇒ api.makePost(thread, text, me).map(post ⇒
              Redirect(routes.Message.thread(thread.id) + "#bottom")
            ))
        }
      }
  }

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      IfCanMessage {
        Ok(html.message.form(forms.thread, get("username")))
      }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IfCanMessage {
        IOResult {
          implicit val req = ctx.body
          forms.thread.bindFromRequest.fold(
            err ⇒ io(BadRequest(html.message.form(err))),
            data ⇒ api.makeThread(data, me).map(thread ⇒
              Redirect(routes.Message.thread(thread.id))
            ))
        }
      }
  }

  def delete(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IORedirect {
        api.deleteThread(id, me) map { _ ⇒ routes.Message.inbox(1) }
      }
  }

  def IfCanMessage(result: ⇒ Result)(implicit ctx: Context, me: UserModel) =
    me.canMessage.fold(result, notFound)
}
