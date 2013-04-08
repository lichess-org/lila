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

  def apply[M <: SocketMember](
    socket: ActorRef,
    uid: String,
    join: Any)(controller: M ⇒ Controller): Fu[JsSocketHandler] = {

    val baseController: Controller = {
      case ("p", _) ⇒ socket ! Ping(uid)
      case _        ⇒
    }

    def iteratee(member: M) =
      Iteratee.foreach[JsValue] { jsv ⇒
        jsv.asOpt[JsObject] foreach { obj ⇒
          obj str "t" foreach { t ⇒
            ~(controller(member) orElse baseController).lift(t -> obj)
          }
        }
      } mapDone { _ ⇒ socket ! Quit(uid) }

    (socket ? join map {
      case Connected(enumerator, member: M) ⇒ iteratee(member) -> enumerator
    }) recover {
      case t: Throwable ⇒ errorHandler(t.getMessage)
    }
  }

  def errorHandler(err: String): JsSocketHandler =
    Iteratee.skipToEof[JsValue] ->
      Enumerator[JsValue](Json.obj(
        "error" -> "Invalid socket request: %s".format(err)
      )).andThen(Enumerator.eof)

}
