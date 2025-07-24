package controllers

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.misc.oauth.AccessTokenId
import lila.push.WebSubscription
import lila.oauth.AccessToken

final class Push(env: Env) extends LilaController(env):

  def mobileRegister(platform: String, deviceId: String) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    env.push.registerDevice(me, platform, deviceId).inject(NoContent)
  }

  def mobileUnregister = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    env.push.unregisterDevices(me).inject(NoContent)
  }

  def webSubscribe = AuthOrScopedBodyWithParser(parse.json)(_.Web.Mobile) { ctx ?=> me ?=>
    val currentSessionId =
      if ctx.isMobileOauth then HTTPRequest.bearer(ctx.req).map(AccessToken.idFrom)
      else env.security.api.reqSessionId(ctx.req)

    currentSessionId match
      case Some(currentSessionId) =>
        ctx.body.body
          .validate[WebSubscription]
          .fold(
            err => BadRequest(err.toString),
            data => env.push.webSubscriptionApi.subscribe(me, data, currentSessionId).inject(NoContent)
          )
      case None => BadRequest("Session ID is missing")
  }
