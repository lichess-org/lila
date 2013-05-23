package lila.socket

import actorApi._
import lila.hub.actorApi.relation.{ GetFriends, FriendsOf }
import makeTimeout.large
import lila.common.PimpedJson._

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import play.api.libs.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator)]

  def apply(
    hub: lila.hub.Env,
    socket: ActorRef,
    uid: String,
    join: Any,
    userId: Option[String])(connecter: Connecter): Fu[JsSocketHandler] = {

    val baseController: Controller = {
      case ("p", _) ⇒ socket ! Ping(uid)
      case ("friends", _) ⇒ userId foreach { u ⇒
        hub.actor.relation ? GetFriends(u) mapTo manifest[List[String]] map { friends ⇒
          FriendsOf(uid, friends)
        } pipeTo socket
      }
      case msg ⇒ logwarn("Unhandled msg: " + msg)
    }

    def iteratee(controller: Controller) = {
      val control = controller orElse baseController
      Iteratee.foreach[JsValue](jsv ⇒
        jsv.asOpt[JsObject] foreach { obj ⇒
          obj str "t" foreach { t ⇒
            ~control.lift(t -> obj)
          }
        }
      ).mapDone(_ ⇒ socket ! Quit(uid))
    }

    (socket ? join map connecter map {
      case (controller, enum) ⇒ iteratee(controller) -> enum
    }) recover {
      case t: Exception ⇒ errorHandler(t.getMessage)
    }
  }

  def errorHandler(err: String): JsSocketHandler =
    Iteratee.skipToEof[JsValue] ->
      Enumerator[JsValue](Json.obj(
        "error" -> "Socket handler error: %s".format(err)
      )).andThen(Enumerator.eof)

}
