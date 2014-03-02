package lila.monitor

import akka.actor._

import actorApi._
import lila.socket.actorApi.Connected
import lila.socket.Handler

private[monitor] final class SocketHandler(
    socket: ActorRef,
    hub: lila.hub.Env) {

  def apply(uid: String): Fu[JsSocketHandler] = {

    def controller: Handler.Controller = {
      case _ =>
    }

    Handler(hub, socket, uid, Join(uid), none) {
      case Connected(enum, member) => controller -> enum
    }
  }
}
