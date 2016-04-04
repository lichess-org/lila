package lila.socket

import akka.actor._
import play.api.libs.json.JsObject

// implements ActorFlow.actorRef out actor
final class SocketMemberActor(
    out: ActorRef,
    handler: JsObject => Unit) extends Actor {

  import SocketMemberActor._

  def receive = {

    // case Send(msg)    => out ! msg

    case msg: JsObject => handler(msg)
  }
}

object SocketMemberActor {

  // case class Send(msg: JsObject)

  def props(out: ActorRef, handler: JsObject => Unit) =
    Props(new SocketMemberActor(out, handler))
}
