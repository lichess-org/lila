package lila.site

import scala.concurrent.duration._

import akka.actor._
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.socket._
import lila.socket.actorApi.StartWatching

private[site] final class SocketHandler(
    socket: ActorRef,
    hub: lila.hub.Env) {

  def apply(
    uid: String,
    userId: Option[String],
    flag: Option[String]): Fu[JsSocketHandler] = {

    def controller(member: Member): Handler.Controller = {
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
    }

    Handler(hub, socket, uid, Join(uid, userId, flag), userId) {
      case Connected(enum, member) => controller(member) -> enum
    }
  }
}
