package controllers

import lila.app.{ *, given }
import lila.push.WebSubscription

final class Push(env: Env) extends LilaController(env):

  def mobileRegister(platform: String, deviceId: String) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    env.push.registerDevice(me, platform, deviceId).inject(NoContent)
  }

  def mobileUnregister = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    env.push.unregisterDevices(me).inject(NoContent)
  }

  def webSubscribe = AuthBody(parse.json) { ctx ?=> me ?=>
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    ctx.body.body
      .validate[WebSubscription]
      .fold(
        err => BadRequest(err.toString),
        data => env.push.webSubscriptionApi.subscribe(me, data, currentSessionId).inject(NoContent)
      )
  }
