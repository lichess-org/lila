package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser.lightUserWrites

final class Msg(
    env: Env
) extends LilaController(env) {

  def home = Auth { implicit ctx => me =>
    ctx.hasInbox ?? negotiate(
      html =
        inboxJson(me) map { json =>
          Ok(views.html.msg.home(json))
        },
      api = v => {
        if (v >= 5) inboxJson(me)
        else env.msg.compat.inbox(me, getInt("page"))
      } map { Ok(_) }
    )
  }

  def threadWith(username: String) = Auth { implicit ctx => me =>
    if (username == "new") Redirect(get("user").fold(routes.Msg.home()) { routes.Msg.threadWith(_) }).fuccess
    else
      ctx.hasInbox ?? {
        env.msg.api.convoWith(me, username) flatMap { convo =>
          def newJson = inboxJson(me).map { _ + ("convo" -> env.msg.json.convo(convo)) }
          negotiate(
            html =
              if (convo.contact.id == me.id) Redirect(routes.Msg.home).fuccess
              else
                newJson map { json =>
                  Ok(views.html.msg.home(json))
                },
            api = v =>
              if (convo.contact.id == me.id) notFoundJson()
              else {
                if (v >= 5) newJson
                else fuccess(env.msg.compat.thread(me, convo))
              } map { Ok(_) }
          )
        }
      }
  }

  def search(q: String) = Auth { ctx => me =>
    ctx.hasInbox ?? {
      q.trim.some.filter(_.size > 1).filter(lila.user.User.couldBeUsername) match {
        case None    => env.msg.json.searchResult(me)(env.msg.search.empty) map { Ok(_) }
        case Some(q) => env.msg.search(me, q) flatMap env.msg.json.searchResult(me) map { Ok(_) }
      }
    }
  }

  def unreadCount = Auth { ctx => me =>
    JsonOk {
      ctx.hasInbox ?? {
        env.msg.api unreadCount me
      }
    }
  }

  def threadDelete(username: String) = Auth { ctx => me =>
    ctx.hasInbox ?? {
      env.msg.api.delete(me, username) >>
        inboxJson(me) map { Ok(_) }
    }
  }

  def compatCreate = AuthBody { implicit ctx => me =>
    ctx.hasInbox ?? {
      env.msg.compat
        .create(me)(ctx.body)
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
          ctx.hasInbox ?? {
            env.msg.compat
              .reply(me, userId)(ctx.body)
              .fold(
                jsonFormError,
                _ inject Ok(Json.obj("ok" -> true, "id" -> userId))
              )
          },
      // new API: create/reply
      scoped = implicit req =>
        me =>
          !me.kid ?? {
            import play.api.data._
            import play.api.data.Forms._
            Form(single("text" -> nonEmptyText)).bindFromRequest
              .fold(
                err => jsonFormErrorFor(err, req, me.some),
                text => env.msg.api.post(me.id, userId, text)
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
