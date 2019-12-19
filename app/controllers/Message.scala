package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc.Result
import scala.concurrent.duration._
import scalatags.Text.Frag

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.security.Granter
import lila.user.{ User => UserModel }
import views._

final class Message(env: Env) extends LilaController(env) {

  private def api         = env.message.api
  private def security    = env.message.security
  private def forms       = env.message.forms
  private def relationApi = env.relation.api

  def inbox(page: Int) = Auth { implicit ctx => me =>
    NotForKids {
      for {
        pag <- api.inbox(me, page)
        _   <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
        res <- negotiate(
          html = fuccess(html.message.inbox(me, pag)),
          api = _ => fuccess(env.message.jsonView.inbox(me, pag))
        )
      } yield res
    }
  }

  def unreadCount = Auth { implicit ctx => me =>
    NotForKids {
      negotiate(
        html = notFound,
        api = _ => JsonOk(api unreadCount me)
      )
    }
  }

  def thread(id: String) = Auth { implicit ctx => implicit me =>
    NotForKids {
      negotiate(
        html = OptionFuOk(api.thread(id, me)) { thread =>
          relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
            val form = thread.isReplyable option forms.post
            html.message.thread(thread, form, blocked)
          }
        } map NoCache,
        api = _ =>
          JsonOptionFuOk(api.thread(id, me)) { thread =>
            env.message.jsonView.thread(thread)
          }
      )
    }
  }

  def answer(id: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(api.thread(id, me) map (_.filterNot(_.isTooBig))) { thread =>
      implicit val req = ctx.body
      negotiate(
        html = forms.post.bindFromRequest.fold(
          err =>
            relationApi.fetchBlocks(thread otherUserId me, me.id) map { blocked =>
              BadRequest(html.message.thread(thread, err.some, blocked))
            },
          text =>
            api.makePost(thread, text, me) inject Redirect {
              s"${routes.Message.thread(thread.id)}#bottom"
            }
        ),
        api = _ =>
          forms.post.bindFromRequest.fold(
            _ => fuccess(BadRequest(Json.obj("err" -> "Malformed request"))),
            text => api.makePost(thread, text, me) inject Ok(Json.obj("ok" -> true, "id" -> thread.id))
          )
      )
    }
  }

  def form = Auth { implicit ctx => implicit me =>
    NotForKids {
      renderForm(me, get("title"), identity) map { Ok(_) }
    }
  }

  private val ThreadLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 20,
    duration = 24 hour,
    name = "PM thread per user",
    key = "pm_thread.user"
  )

  private val ThreadLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 30,
    duration = 24 hour,
    name = "PM thread per IP",
    key = "pm_thread.ip"
  )

  implicit private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Message.inbox(1)))
  }

  def create = AuthBody { implicit ctx => implicit me =>
    NotForKids {
      env.chat.panic.allowed(me) ?? {
        implicit val req = ctx.body
        negotiate(
          html = forms
            .thread(me)
            .bindFromRequest
            .fold(
              err => renderForm(me, none, _ => err) map { BadRequest(_) },
              data => {
                val cost =
                  if (isGranted(_.ModMessage)) 0
                  else if (!me.createdSinceDays(3)) 2
                  else 1
                ThreadLimitPerUser(me.id, cost = cost) {
                  ThreadLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = cost) {
                    api.makeThread(data, me) map { thread =>
                      if (thread.asMod)
                        env.mod.logApi.modMessage(thread.creatorId, thread.invitedId, thread.name)
                      Redirect(routes.Message.thread(thread.id))
                    }
                  }
                }
              }
            ),
          api = _ =>
            forms
              .thread(me)
              .bindFromRequest
              .fold(
                jsonFormError,
                data =>
                  api.makeThread(data, me) map { thread =>
                    Ok(Json.obj("ok" -> true, "id" -> thread.id))
                  }
              )
        )
      }
    }
  }

  private def renderForm(me: UserModel, title: Option[String], f: Form[_] => Form[_])(
      implicit ctx: Context
  ): Fu[Frag] =
    get("user") ?? env.user.repo.named flatMap { user =>
      user.fold(fuTrue)(u => security.canMessage(me.id, u.id)) map { canMessage =>
        html.message.form(
          f(forms thread me),
          reqUser = user,
          reqTitle = title,
          canMessage = canMessage || Granter(_.MessageAnyone)(me),
          oldEnough = env.chat.panic.allowed(me)
        )
      }
    }

  def batch = AuthBody { implicit ctx => implicit me =>
    val ids = get("ids").??(_.split(",").toList).distinct take 200
    env.message.batch(me, ~get("action"), ids) inject Redirect(routes.Message.inbox(1))
  }

  def delete(id: String) = AuthBody { implicit ctx => implicit me =>
    negotiate(
      html = api.deleteThread(id, me) inject Redirect(routes.Message.inbox(1)),
      api = _ => api.deleteThread(id, me) inject Ok(Json.obj("ok" -> true))
    )
  }
}
