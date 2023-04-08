package controllers

import lila.app.{ given, * }
import lila.push.WebSubscription

final class Push(env: Env) extends LilaController(env):

  def mobileRegister(platform: String, deviceId: String) =
    Auth { implicit ctx => me =>
      env.push.registerDevice(me, platform, deviceId)
    }

  def mobileUnregister =
    Auth { implicit ctx => me =>
      env.push.unregisterDevices(me)
    }

  def webSubscribe =
    AuthBody(parse.json) { implicit ctx => me =>
      val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
      ctx.body.body
        .validate[WebSubscription]
        .fold(
          err => BadRequest(err.toString).toFuccess,
          data => env.push.webSubscriptionApi.subscribe(me, data, currentSessionId) inject NoContent
        )
    }
