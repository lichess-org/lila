package lila.socket

import akka.actor._
import play.api.libs.json.JsValue

// implements ActorFlow.actorRef out actor
final class SocketMemberActor(
    out: ActorRef,
    handler: JsValue => Unit) extends Actor {

  import SocketMemberActor._

  def receive = {

    // case Send(msg)    => out ! msg

    case msg: JsValue => handler(msg)
  }
}

object SocketMemberActor {

  // case class Send(msg: JsValue)

  def props(out: ActorRef, handler: JsValue => Unit) =
    Props(new SocketMemberActor(out, handler))
}
