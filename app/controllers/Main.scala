package controllers

import lila._
import views._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

object Main extends LilaController {

  val websocket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = reqToCtx(req)
    env.site.socket.join(
      uidOption = get("uid"),
      username = ctx.me map (_.username))
  }

  val blocked = Action { implicit req ⇒
    Async {
      Akka.future {
        println("BLOCK %s %s".format(req.remoteAddress, req))
        Thread sleep 10 * 1000
        BadRequest(html.base.blocked())
      }
    }
  }
}
