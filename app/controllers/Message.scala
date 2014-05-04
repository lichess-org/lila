package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import lila.app._
import lila.user.{ User => UserModel, UserRepo }
import views._

object Message extends LilaController {

  private def api = Env.message.api
  private def forms = Env.message.forms
  private def relationApi = Env.relation.api

  def inbox(page: Int) = Auth { implicit ctx =>
    me =>
      api updateUser me.id
      api.inbox(me, page) map { html.message.inbox(me, _) }
  }

  def preview = Auth { implicit ctx =>
    me => api.preview(me.id) map { html.message.preview(me, _) }
  }

  def thread(id: String) = Auth { implicit ctx =>
    implicit me =>
      OptionFuOk(api.thread(id, me)) { thread =>
        relationApi.blocks(thread otherUserId me, me.id) map { blocked =>
          html.message.thread(thread, forms.post, blocked)
        }
      }
  }

  def answer(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      OptionFuResult(api.thread(id, me)) { thread =>
        implicit val req = ctx.body
        forms.post.bindFromRequest.fold(
          err => relationApi.blocks(thread otherUserId me, me.id) map { blocked =>
            BadRequest(html.message.thread(thread, err, blocked))
          },
          text => api.makePost(thread, text, me) inject Redirect(routes.Message.thread(thread.id) + "#bottom")
        )
      }
  }

  def form = Auth { implicit ctx =>
    implicit me =>
      get("username") ?? UserRepo.named map { user =>
        Ok(html.message.form(forms.thread(me), user))
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      implicit val req = ctx.body
      forms.thread(me).bindFromRequest.fold(
        err => get("username") ?? UserRepo.named map { user =>
          BadRequest(html.message.form(err, user))
        },
        data => api.makeThread(data, me) map { thread =>
          Redirect(routes.Message.thread(thread.id))
        })
  }

  def delete(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1))
  }
}
