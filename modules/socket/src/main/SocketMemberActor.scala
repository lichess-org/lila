package lila.socket

import akka.actor._
import play.api.libs.json.JsValue

final class SocketMemberActor(out: ActorRef) extends Actor {

  import SocketMemberActor._

  def receive = {

    case Out(msg)     => out ! msg

    case msg: JsValue =>
    // out ! ("I received your message: " + msg)
  }
}

object SocketMemberActor {

  case class Out(msg: JsValue)
}
