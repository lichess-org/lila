package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.core.net.ApiVersion

final class Msg(env: Env) extends LilaController(env):

  private val newMobileApi = ApiVersion(5)

  def home = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    negotiateApi(
      html = Ok.async(inboxJson.map(views.msg.home)).map(_.hasPersonalData),
      api = v =>
        JsonOk:
          if v >= newMobileApi then inboxJson
          else env.msg.compat.inbox(getInt("page"))
    )
  }

  def convo(username: UserStr, before: Option[Long] = None) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    if username.is(UserStr("new"))
    then Redirect(getUserStr("user").fold(routes.Msg.home)(routes.Msg.convo(_)))
    else
      env.msg.api
        .convoWithMe(username, before)
        .flatMap:
          case None    => negotiate(Redirect(routes.Msg.home), notFoundJson())
          case Some(c) =>
            def newJson = inboxJson.map { _ + ("convo" -> env.msg.json.convo(c)) }
            negotiateApi(
              html = Ok.async(newJson.map(views.msg.home)),
              api = v =>
                JsonOk:
                  if v >= newMobileApi then newJson
                  else fuccess(env.msg.compat.thread(c))
            ).map(_.hasPersonalData)
  }

  def search(q: String) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    JsonOk:
      q.trim.some.filter(_.nonEmpty) match
        case None    => env.msg.json.searchResult(env.msg.search.empty)
        case Some(q) => env.msg.search(q).flatMap(env.msg.json.searchResult)
  }

  def unreadCountBC = Auth { _ ?=> me ?=>
    JsonOk:
      env.msg.compat.unreadCount(me)
  }

  def convoDelete(username: UserStr) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    env.msg.api.delete(username) >>
      JsonOk(inboxJson)
  }

  def compatCreate = AuthBody { ctx ?=> me ?=>
    ctx.kid.no
      .so(ctx.noBot)
      .so(
        env.msg.compat.create
          .fold(
            doubleJsonFormError,
            _.map: id =>
              Ok(Json.obj("ok" -> true, "id" -> id))
          )
      )
  }

  def apiPost(username: UserStr) = AuthOrScopedBody(_.Msg.Write) { ctx ?=> me ?=>
    val userId = username.id
    if ctx.isWebAuth then // compat: reply
      env.msg.compat
        .reply(userId)
        .fold(doubleJsonFormError, _.inject(Ok(Json.obj("ok" -> true, "id" -> userId))))
    else // new API: create/reply
      (ctx.kid.no && me.isnt(userId)).so:
        bindForm(env.msg.textForm)(
          doubleJsonFormError,
          text =>
            env.msg.api
              .post(me, userId, text)
              .flatMap:
                case lila.core.msg.PostResult.Success => jsonOkResult
                case lila.core.msg.PostResult.Limited => rateLimited
                case _                                => BadRequest(jsonError("The message was rejected"))
        )
  }

  private def inboxJson(using me: Me) =
    import lila.common.Json.lightUserWrites
    for threads <- env.msg.api.myThreads.flatMap(env.msg.json.threads)
    yield Json.obj(
      "me"       -> Json.toJsObject(me.light).add("bot" -> me.isBot),
      "contacts" -> threads
    )
