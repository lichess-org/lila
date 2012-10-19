package controllers

import lila._
import views._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

object Main extends LilaController {

  def websocket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = reqToCtx(req)
    env.site.socket.join(
      uidOption = get("sri"),
      username = ctx.me map (_.username),
      flag = get("flag")
    )
  }
}
