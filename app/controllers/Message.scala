package controllers

import play.api._
import play.api.data.Form
import play.api.mvc._
import play.api.mvc.Results._
import play.twirl.api.Html
import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.security.Granter
import lila.user.{ User => UserModel, UserRepo }
import views._

object Message extends LilaController {

  private def api = Env.message.api
  private def security = Env.message.security
  private def forms = Env.message.forms
  private def relationApi = Env.relation.api

  def inbox(page: Int) = Auth { implicit ctx =>
    me =>
      NotForKids {
        negotiate (
          html = api.inbox(me, page) map { html.message.inbox(me, _) },
          api = _ => api.inbox(me, page) map { Env.message.jsonView.inbox(me, _) }
        )
      }
  }

  def thread(id: String) = Auth { implicit ctx =>
    implicit me =>
      NotForKids {
        negotiate (
          html = OptionFuOk(api.thread(id, me)) { thread =>
            relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
              html.message.thread(thread, forms.post, blocked)
            }
          } map NoCache,
          api = _ => JsonOptionFuOk(api.thread(id, me)) { thread => Env.message.jsonView.thread(thread) }
        )
      }
  }

  def answer(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      negotiate (
        html = OptionFuResult(api.thread(id, me)) { thread =>
          implicit val req = ctx.body
          forms.post.bindFromRequest.fold(
            err => relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
              BadRequest(html.message.thread(thread, err, blocked))
            },
            text => api.makePost(thread, text, me) inject Redirect(routes.Message.thread(thread.id) + "#bottom")
          )
        },
        api = _ => OptionFuResult(api.thread(id, me)) { thread =>
          implicit val req = ctx.body
          forms.post.bindFromRequest.fold(
            err => relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
              BadRequest(html.message.thread(thread, err, blocked))
            },
            text => api.makePost(thread, text, me) inject Ok(Json.obj("ok" -> true, "id" -> thread.id))
          )
        }
      )
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
        negotiate (
          html = forms.thread(me).bindFromRequest.fold(
            err => renderForm(me, none, _ => err) map { BadRequest(_) },
            data => api.makeThread(data, me) map { thread =>
              Redirect(routes.Message.thread(thread.id))
            }),
          api = _ => forms.thread(me).bindFromRequest.fold(
            err => renderForm(me, none, _ => err) map { BadRequest(_) },
            data => api.makeThread(data, me) map { thread =>
              Ok(Json.obj("ok" -> true, "id" -> thread.id))
            })
        )
      }
  }

  private def renderForm(me: UserModel, title: Option[String], f: Form[_] => Form[_])(implicit ctx: Context): Fu[Html] =
    get("user") ?? UserRepo.named flatMap { user =>
      user.fold(fuccess(true))(u => security.canMessage(me.id, u.id)) map { canMessage =>
        html.message.form(f(forms thread me), user, title,
          canMessage = canMessage || Granter(_.MessageAnyone)(me))
      }
    }

  def batch = AuthBody { implicit ctx =>
    implicit me =>
      val ids = get("ids").??(_.split(",").toList).distinct take 200
      Env.message.batch(me, ~get("action"), ids) inject Redirect(routes.Message.inbox(1))
  }

  def delete(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      negotiate (
        html = api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1)),
        api = _ => api.deleteThread(id, me) inject Ok(Json.obj("ok" -> true, "id" -> id))
      )
  }
}
