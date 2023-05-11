package controllers

import play.api.libs.json.*

import lila.app.{ given, * }
import lila.common.LightUser.lightUserWrites

final class Msg(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me =>
    negotiate(
      html = inboxJson(me) map { json =>
        Ok(views.html.msg.home(json))
      },
      api = v =>
        {
          if (v.value >= 5) inboxJson(me)
          else env.msg.compat.inbox(me, getInt("page"))
        } map { Ok(_) }
    )
  }

  def convo(username: UserStr, before: Option[Long] = None) = Auth { _ ?=> me =>
    if (username.value == "new") Redirect(get("user").fold(routes.Msg.home)(routes.Msg.convo(_))).toFuccess
    else
      env.msg.api.convoWith(me, username, before).flatMap {
        case None =>
          negotiate(
            html = Redirect(routes.Msg.home).toFuccess,
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
                if (v.value >= 5) newJson
                else fuccess(env.msg.compat.thread(me, c))
              } map { Ok(_) }
          )
      }
  }

  def search(q: String) = Auth { _ ?=> me =>
    q.trim.some.filter(_.nonEmpty) match
      case None    => env.msg.json.searchResult(me)(env.msg.search.empty) map { Ok(_) }
      case Some(q) => env.msg.search(me, q) flatMap env.msg.json.searchResult(me) map { Ok(_) }
  }

  def unreadCount = Auth { _ ?=> me =>
    JsonOk:
      env.msg.compat unreadCount me
  }

  def convoDelete(username: UserStr) = Auth { _ ?=> me =>
    env.msg.api.delete(me, username) >>
      inboxJson(me) map { Ok(_) }
  }

  def compatCreate = AuthBody { ctx ?=> me =>
    ctx.noKid ?? ctx.noBot ?? {
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

  def apiPost(username: UserStr) =
    val userId = username.id
    AuthOrScopedBody(_.Msg.Write)(
      // compat: reply
      auth = ctx ?=>
        me =>
          env.msg.compat
            .reply(me, userId)
            .fold(
              jsonFormError,
              _ inject Ok(Json.obj("ok" -> true, "id" -> userId))
            ),
      // new API: create/reply
      scoped = req ?=>
        me =>
          (!me.kid && !me.is(userId)) ?? {
            import play.api.data.*
            import play.api.data.Forms.*
            Form(single("text" -> nonEmptyText))
              .bindFromRequest()
              .fold(
                err => jsonFormErrorFor(err, req, me.some),
                text =>
                  env.msg.api.post(me.id, userId, text) map {
                    case lila.msg.MsgApi.PostResult.Success => jsonOkResult
                    case lila.msg.MsgApi.PostResult.Limited => rateLimitedJson
                    case _ => BadRequest(jsonError("The message was rejected"))
                  }
              )
          }
    )

  private def inboxJson(me: lila.user.User) =
    env.msg.api.threadsOf(me) flatMap env.msg.json.threads(me) map { threads =>
      Json.obj(
        "me"       -> lightUserWrites.writes(me.light).add("bot" -> me.isBot),
        "contacts" -> threads
      )
    }
