package lila.socket

import actorApi.SocketLeave
import akka.actor._
import play.api.libs.json.JsValue

final class Channel extends Actor {

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'socketDoor)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  import Channel._

  val members = scala.collection.mutable.Set.empty[SocketMember]

  def receive = {

    case Sub(member)            => members += member

    case UnSub(member)          => members -= member

    case SocketLeave(_, member) => members -= member

    case Publish(msg)           => members.foreach(_ push msg)
  }
}

object Channel {

  case class Sub(member: SocketMember)
  case class UnSub(member: SocketMember)

  case class Publish(msg: JsValue)
}
