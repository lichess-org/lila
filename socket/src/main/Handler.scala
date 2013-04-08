package lila.socket

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Execution.Implicits._

import actorApi._
import makeTimeout.large
import lila.common.PimpedJson._

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]

  def apply(socket: ActorRef, join: Any, quit: Any)(controller: Controller): Fu[JsSocketHandler] = (socket ? join map {
    case Connected(enumerator, member) ⇒
      (makeIteratee(controller) mapDone { _ ⇒ socket ! quit }) -> enumerator
  }) recover {
    case t: Throwable ⇒ errorHandler(t.getMessage)
  }

  def errorHandler(err: String): JsSocketHandler =
    Iteratee.skipToEof[JsValue] ->
      Enumerator[JsValue](Json.obj(
        "error" -> "Invalid socket request: %s".format(err)
      )).andThen(Enumerator.eof)

  private def makeIteratee(controller: Controller) =
    Iteratee.foreach[JsValue] { jsv ⇒
      jsv.asOpt[JsObject] foreach { obj ⇒
        obj str "t" foreach { t ⇒
          ~controller.lift(t -> obj)
        }
      }
    }
}
