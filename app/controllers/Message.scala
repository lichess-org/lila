package controllers

import lila.app._
import views._
import lila.user.{ User ⇒ UserModel, Context }

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Message extends LilaController {

  private def api = Env.message.api
  private def forms = Env.message.forms

  def inbox(page: Int) = Auth { implicit ctx ⇒
    implicit me ⇒ api.inbox(me, page) map { html.message.inbox(_) }
  }

  def thread(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      OptionOk(api.thread(id, me)) { html.message.thread(_, forms.post) }
  }

  def answer(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IfCanMessage {
        OptionFuResult(api.thread(id, me)) { thread ⇒
          implicit val req = ctx.body
          forms.post.bindFromRequest.fold(
            err ⇒ BadRequest(html.message.thread(thread, err)).fuccess,
            text ⇒ api.makePost(thread, text, me) inject Redirect(routes.Message.thread(thread.id) + "#bottom")
          )
        }
      }
  }

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      IfCanMessage {
        Ok(html.message.form(forms.thread, get("username"))).fuccess
      }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IfCanMessage {
        implicit val req = ctx.body
        forms.thread.bindFromRequest.fold(
          err ⇒ BadRequest(html.message.form(err)).fuccess,
          data ⇒ api.makeThread(data, me) map { thread ⇒
            Redirect(routes.Message.thread(thread.id))
          })
      }
  }

  def delete(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1))
  }

  def IfCanMessage(result: ⇒ Fu[Result])(implicit ctx: Context, me: UserModel) =
    me.canMessage.fold(result, notFound)
}
