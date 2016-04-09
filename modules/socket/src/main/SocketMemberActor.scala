package lila.socket

import akka.actor._
import play.api.libs.json.JsObject

// implements ActorFlow.actorRef out actor
final class SocketMemberActor private(
    handler: JsObject => Unit,
    onClose: () => Unit) extends Actor {

  override def postStop() = onClose()

  def receive = {

    case msg: JsObject => handler(msg)

    case m             => lila.log("socket").warn(s"SocketMemberActor received $m")
  }
}

object SocketMemberActor {

  def props(handler: JsObject => Unit, onClose: () => Unit) =
    Props(new SocketMemberActor(handler, onClose))
}
