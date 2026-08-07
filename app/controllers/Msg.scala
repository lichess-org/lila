package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given

final class Msg(env: Env) extends LilaController(env):

  def home(before: Option[Long] = None) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    before match
      case None =>
        negotiateApi(
          html = Ok.async(inboxJson(none).map(views.msg.home)).map(_.hasPersonalData),
          api = _ => JsonOk(inboxJson(none))
        )
      case Some(before) =>
        JsonOk:
          for
            threads <- env.msg.api.moreContacts(millisToInstant(before))
            contacts <- env.msg.json.contacts(threads)
          yield contacts
  }

  def convo(username: UserStr, before: Option[Long] = None) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    if username.is(UserStr("new"))
    then Redirect(getUserStr("user").fold(routes.Msg.home())(routes.Msg.convo(_)))
    else
      env.msg.api
        .convoWithMe(username, before)
        .flatMap:
          case None => negotiate(Redirect(routes.Msg.home()), notFoundJson())
          case Some(c) =>
            def json = inboxJson(c.contact.id.some).map { _ + ("convo" -> env.msg.json.convo(c)) }
            negotiateApi(
              html = Ok.async(json.map(views.msg.home)),
              api = _ => JsonOk(json)
            ).map(_.hasPersonalData)
  }

  def search(q: String) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    JsonOk:
      q.trim.nonEmptyOption match
        case None => env.msg.json.searchResult(env.msg.search.empty)
        case Some(q) => env.msg.search(q).flatMap(env.msg.json.searchResult)
  }

  def unreadCount = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    JsonOk:
      env.msg.unreadCount.mobile(me)
  }

  def convoDelete(username: UserStr) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    env.msg.api.delete(username) >>
      JsonOk(inboxJson(none))
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
                case _ => BadRequest(jsonError("The message was rejected"))
        )
  }

  private def inboxJson(withConvo: Option[UserId])(using me: Me) =
    import lila.common.Json.lightUserWrites
    for
      threads <- env.msg.api.myThreads
      contactIds = withConvo.toList ::: threads.map(_.other)
      studentNames <- env.clas.api.clas.myPotentialStudentNames(contactIds)
      contacts <- env.msg.json.threadsJson(threads)
    yield Json.obj(
      "me" -> Json.toJsObject(me.light).add("bot" -> me.isBot),
      "contacts" -> contacts,
      "names" -> studentNames
    )
