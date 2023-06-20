package controllers

import play.api.libs.json.*

import lila.app.{ given, * }
import lila.common.LightUser.lightUserWrites

final class Msg(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    negotiate(
      html = inboxJson.map(views.html.msg.home),
      api = v =>
        JsonOk:
          if v.value >= 5 then inboxJson
          else env.msg.compat.inbox(getInt("page"))
    )
  }

  def convo(username: UserStr, before: Option[Long] = None) = Auth { _ ?=> me ?=>
    if username.value == "new"
    then Redirect(get("user").fold(routes.Msg.home)(routes.Msg.convo(_)))
    else
      env.msg.api.convoWithMe(username, before).flatMap {
        case None =>
          negotiate(
            html = Redirect(routes.Msg.home),
            api = _ => notFoundJson()
          )
        case Some(c) =>
          def newJson = inboxJson.map { _ + ("convo" -> env.msg.json.convo(c)) }
          negotiate(
            html = newJson map views.html.msg.home,
            api = v =>
              JsonOk:
                if v.value >= 5 then newJson
                else fuccess(env.msg.compat.thread(c))
          )
      }
  }

  def search(q: String) = Auth { _ ?=> me ?=>
    JsonOk:
      q.trim.some.filter(_.nonEmpty) match
        case None    => env.msg.json.searchResult(env.msg.search.empty)
        case Some(q) => env.msg.search(q) flatMap env.msg.json.searchResult
  }

  def unreadCount = Auth { _ ?=> me ?=>
    JsonOk:
      env.msg.compat unreadCount me
  }

  def convoDelete(username: UserStr) = Auth { _ ?=> me ?=>
    env.msg.api.delete(username) >>
      JsonOk(inboxJson)
  }

  def compatCreate = AuthBody { ctx ?=> me ?=>
    ctx.noKid so ctx.noBot so env.msg.compat.create
      .fold(
        jsonFormError,
        _.map: id =>
          Ok(Json.obj("ok" -> true, "id" -> id))
      )
  }

  def apiPost(username: UserStr) =
    val userId = username.id
    AuthOrScopedBody(_.Msg.Write)(
      // compat: reply
      auth = ctx ?=>
        me ?=>
          env.msg.compat
            .reply(userId)
            .fold(
              jsonFormError,
              _ inject Ok(Json.obj("ok" -> true, "id" -> userId))
            ),
      // new API: create/reply
      scoped = ctx ?=>
        me ?=>
          (!me.kid && !me.is(userId)) so {
            import play.api.data.*
            import play.api.data.Forms.*
            Form(single("text" -> nonEmptyText))
              .bindFromRequest()
              .fold(
                jsonFormError,
                text =>
                  env.msg.api.post(me, userId, text) map {
                    case lila.msg.MsgApi.PostResult.Success => jsonOkResult
                    case lila.msg.MsgApi.PostResult.Limited => rateLimitedJson
                    case _ => BadRequest(jsonError("The message was rejected"))
                  }
              )
          }
    )

  private def inboxJson(using me: Me) =
    env.msg.api.myThreads flatMap env.msg.json.threads map { threads =>
      Json.obj(
        "me"       -> lightUserWrites.writes(me.light).add("bot" -> me.isBot),
        "contacts" -> threads
      )
    }
