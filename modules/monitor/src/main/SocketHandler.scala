package lila.monitor

import akka.actor._

import lila.socket._
import lila.socket.actorApi.Connected

private[monitor] final class SocketHandler(socket: ActorRef) {

  def join(uid: String): Fu[JsSocketHandler] = {

    def controller: Handler.Controller = {
      case _ =>
    }

    Handler(socket, uid, Join(uid)) {
      case Connected(enum, member) ⇒ controller -> enum
    }
  }
}
