package lila.monitor

import akka.actor._

import lila.socket._

private[monitor] final class SocketHandler(socket: ActorRef) {

  def join(uid: String): Fu[JsSocketHandler] =
    Handler(socket, uid, Join(uid)) {
      case _ ⇒
    }
}
