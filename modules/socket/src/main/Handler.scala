package lila.socket

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }

import actorApi._
import makeTimeout.large
import lila.common.PimpedJson._

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator)]

  def apply[M <: SocketMember](
    socket: ActorRef,
    uid: String,
    join: Any)(connecter: Connecter): Fu[JsSocketHandler] = {

    val baseController: Controller = {
      case ("p", _) ⇒ socket ! Ping(uid)
      case _        ⇒
    }

    def iteratee(controller: Controller) = {
      val control = controller orElse baseController
      Iteratee.foreach[JsValue](jsv ⇒
        jsv.asOpt[JsObject] foreach { obj ⇒
          obj str "t" foreach { t ⇒
            control(t -> obj)
            // TODO handle errors (with lift?)
            // ~control.lift(t -> obj)
          }
        }
      ).mapDone(_ ⇒ socket ! Quit(uid))
    }

    (socket ? join map connecter map {
      case (controller, enum) ⇒ iteratee(controller) -> enum
    }) recover {
      case t: Throwable ⇒ errorHandler(t.getMessage)
    }
  }

  def errorHandler(err: String): JsSocketHandler = {
    logwarn("[socket] " + err)
    Iteratee.skipToEof[JsValue] ->
      Enumerator[JsValue](Json.obj(
        "error" -> "Socket handler error: %s".format(err)
      )).andThen(Enumerator.eof)
  }
}
