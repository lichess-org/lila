package lila.socket

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.relation.ReloadOnlineFriends
import makeTimeout.large

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
      case ("p", _) => socket ! Ping(uid)
      case ("following_onlines", _) => userId foreach { u =>
        hub.actor.relation ! ReloadOnlineFriends(u)
      }
      case msg => logwarn("Unhandled msg: " + msg)
    }

    def iteratee(controller: Controller): JsIteratee = {
      val control = controller orElse baseController
      Iteratee.foreach[JsValue](jsv =>
        jsv.asOpt[JsObject] foreach { obj =>
          obj str "t" foreach { t =>
            control.lift(t -> obj)
          }
        }
      ).map(_ => socket ! Quit(uid))
    }

    (socket ? join map connecter map {
      case (controller, enum) => iteratee(controller) -> enum
    }) recover {
      case t: Exception => errorHandler(t.getMessage)
    }
  }

  def errorHandler(err: String): JsSocketHandler =
    Iteratee.skipToEof[JsValue] ->
      Enumerator[JsValue](Json.obj(
        "error" -> "Socket handler error: %s".format(err)
      )).andThen(Enumerator.eof)

}
