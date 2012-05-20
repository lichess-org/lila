package controllers

import lila._
import views._

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._

object Main extends LilaController {

  val websocket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = reqToCtx(req)
    env.site.socket.join(
      uidOption = get("uid"),
      username = ctx.me map (_.username))
  }
}
