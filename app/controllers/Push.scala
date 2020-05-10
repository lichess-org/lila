package controllers

import play.api.mvc._

import lidraughts.app._
import lidraughts.push.WebSubscription
import lidraughts.push.WebSubscription.readers._

object Push extends LidraughtsController {

  def mobileRegister(platform: String, deviceId: String) = Auth { implicit ctx => me =>
    Env.push.registerDevice(me, platform, deviceId)
  }

  def mobileUnregister = Auth { implicit ctx => me =>
    Env.push.unregisterDevices(me)
  }

  def webSubscribe = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    val currentSessionId = ~Env.security.api.reqSessionId(ctx.req)
    ctx.body.body.validate[WebSubscription].fold(
      err => BadRequest(err.toString).fuccess,
      data => Env.push.webSubscriptionApi.subscribe(me, data, currentSessionId) inject NoContent
    )
  }
}
