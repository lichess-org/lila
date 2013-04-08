package lila.monitor

import akka.actor._

import lila.socket._
import lila.socket.actorApi.{ Ping, Quit }

private[monitor] final class SocketHandler(hub: ActorRef) {

  def join(uid: String): Fu[JsSocketHandler] =
    Handler(hub, Join(uid), Quit(uid)) {
      case ("p", _) ⇒ hub ! Ping(uid)
    }
}
