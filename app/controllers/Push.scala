package controllers

import lila.app._
import lila.push.WebSubscription
import lila.push.WebSubscription.readers._

final class Push(env: Env) extends LilaController(env) {

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
      val currentSessionId = ~lila.common.HTTPRequest.userSessionId(ctx.req)
      ctx.body.body
        .validate[WebSubscription]
        .fold(
          err => BadRequest(err.toString).fuccess,
          data => env.push.webSubscriptionApi.subscribe(me, data, currentSessionId) inject NoContent
        )
    }
}
