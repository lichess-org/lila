package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser.lightUserWrites

final class Msg(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
      negotiate(
        html = inboxJson(me) map { json =>
          Ok(views.html.msg.home(json))
        },
        api = v =>
          {
            if (v >= 5) inboxJson(me)
            else env.msg.compat.inbox(me, getInt("page"))
          } map { Ok(_) }
      )
    }

  def convo(username: String, before: Option[Long] = None) =
    Auth { implicit ctx => me =>
      if (username == "new") Redirect(get("user").fold(routes.Msg.home)(routes.Msg.convo(_))).fuccess
      else
        env.msg.api.convoWith(me, username, before).flatMap {
          case None =>
            negotiate(
              html = Redirect(routes.Msg.home).fuccess,
              api = _ => notFoundJson()
            )
          case Some(c) =>
            def newJson = inboxJson(me).map { _ + ("convo" -> env.msg.json.convo(c)) }
            negotiate(
              html = newJson map { json =>
                Ok(views.html.msg.home(json))
              },
              api = v =>
                {
                  if (v >= 5) newJson
                  else fuccess(env.msg.compat.thread(me, c))
                } map { Ok(_) }
            )
        }
    }

  def search(q: String) =
    Auth { ctx => me =>
      q.trim.some.filter(lila.user.User.couldBeUsername) match {
        case None    => env.msg.json.searchResult(me)(env.msg.search.empty) map { Ok(_) }
        case Some(q) => env.msg.search(me, q) flatMap env.msg.json.searchResult(me) map { Ok(_) }
      }
    }

  def unreadCount =
    Auth { ctx => me =>
      JsonOk {
        env.msg.api unreadCount me
      }
    }

  def convoDelete(username: String) =
    Auth { _ => me =>
      env.msg.api.delete(me, username) >>
        inboxJson(me) map { Ok(_) }
    }

  def compatCreate =
    AuthBody { implicit ctx => me =>
      ctx.noKid ?? {
        env.msg.compat
          .create(me)(ctx.body, formBinding)
          .fold(
            jsonFormError,
            _ map { id =>
              Ok(Json.obj("ok" -> true, "id" -> id))
            }
          )
      }
    }

  def apiPost(username: String) = {
    val userId = lila.user.User normalize username
    AuthOrScopedBody(_.Msg.Write)(
      // compat: reply
      auth = implicit ctx =>
        me =>
          env.msg.compat
            .reply(me, userId)(ctx.body, formBinding)
            .fold(
              jsonFormError,
              _ inject Ok(Json.obj("ok" -> true, "id" -> userId))
            ),
      // new API: create/reply
      scoped = implicit req =>
        me =>
          (!me.kid && userId != me.id) ?? {
            import play.api.data._
            import play.api.data.Forms._
            Form(single("text" -> nonEmptyText))
              .bindFromRequest()
              .fold(
                err => jsonFormErrorFor(err, req, me.some),
                text =>
                  env.msg.api.post(me.id, userId, text) map {
                    case lila.msg.MsgApi.PostResult.Success => jsonOkResult
                    case lila.msg.MsgApi.PostResult.Limited => apiC.tooManyRequests
                    case _                                  => BadRequest(jsonError("The message was rejected"))
                  }
              )
          }
    )
  }

  private def inboxJson(me: lila.user.User) =
    env.msg.api.threadsOf(me) flatMap env.msg.json.threads(me) map { threads =>
      Json.obj(
        "me"       -> lightUserWrites.writes(me.light).add("kid" -> me.kid),
        "contacts" -> threads
      )
    }
}
