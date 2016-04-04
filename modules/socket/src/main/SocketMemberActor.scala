package lila.socket

import akka.actor._
import play.api.libs.json.JsObject

// implements ActorFlow.actorRef out actor
final class SocketMemberActor(
    handler: JsObject => Unit) extends Actor {

  def receive = {

    case msg: JsObject => handler(msg)

    case m             => lila.log("socket").warn(s"SocketMemberActor received $m")
  }
}

object SocketMemberActor {

  def props(handler: JsObject => Unit) =
    Props(new SocketMemberActor(handler))
}
