package controllers

import play.api._
import play.api.data.Form
import play.api.mvc._
import play.api.mvc.Results._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.user.{ User => UserModel, UserRepo }
import lila.security.Granter
import views._

object Message extends LilaController {

  private def api = Env.message.api
  private def security = Env.message.security
  private def forms = Env.message.forms
  private def relationApi = Env.relation.api

  def inbox(page: Int) = Auth { implicit ctx =>
    me =>
      NotForKids {
        api.inbox(me, page) map { html.message.inbox(me, _) }
      }
  }

  def thread(id: String) = Auth { implicit ctx =>
    implicit me =>
      NotForKids {
        OptionFuOk(api.thread(id, me)) { thread =>
          relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
            html.message.thread(thread, forms.post, blocked,
              answerable = !Env.message.LichessSenders.contains(thread.creatorId))
          }
        } map NoCache
      }
  }

  def answer(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      OptionFuResult(api.thread(id, me)) { thread =>
        implicit val req = ctx.body
        forms.post.bindFromRequest.fold(
          err => relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
            BadRequest(html.message.thread(thread, err, blocked,
              answerable = !Env.message.LichessSenders.contains(thread.creatorId)))
          },
          text => api.makePost(thread, text, me) inject Redirect(routes.Message.thread(thread.id) + "#bottom")
        )
      }
  }

  def form = Auth { implicit ctx =>
    implicit me =>
      NotForKids {
        renderForm(me, get("title"), identity) map { Ok(_) }
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NotForKids {
        implicit val req = ctx.body
        forms.thread(me).bindFromRequest.fold(
          err => renderForm(me, none, _ => err) map { BadRequest(_) },
          data => api.makeThread(data, me) map { thread =>
            Redirect(routes.Message.thread(thread.id))
          })
      }
  }

  private def renderForm(me: UserModel, title: Option[String], f: Form[_] => Form[_])(implicit ctx: Context): Fu[Html] =
    get("user") ?? UserRepo.named flatMap { user =>
      user.fold(fuccess(true))(u => security.canMessage(me.id, u.id)) map { canMessage =>
        html.message.form(f(forms thread me), user, title,
          canMessage = canMessage || Granter(_.MessageAnyone)(me))
      }
    }

  def delete(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1))
  }

  def markAsRead(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      api.markThreadAsRead(id, me)
  }
}
