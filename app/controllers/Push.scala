package controllers

import scala.concurrent.duration._

import play.api.mvc._

import lila.app._
import lila.push.WebSubscription
import lila.push.WebSubscription.readers._

object Push extends LilaController {

  def mobileRegister(platform: String, deviceId: String) = Auth { implicit ctx => me =>
    Env.push.registerDevice(me, platform, deviceId)
  }

  def mobileUnregister = Auth { implicit ctx => me =>
    Env.push.unregisterDevices(me)
  }

  def webSubscribe = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    ctx.body.body.validate[WebSubscription].fold(
      err => BadRequest(err.toString).fuccess,
      data => Env.push.webSubscribe(me, data) >> {
        Env.push.testMessage(me.id)
      } /* Env.current.scheduler.after(2 seconds) {
        Env.push.testMessage(me.id)
        NoContent
      } */
    )
  }
}
