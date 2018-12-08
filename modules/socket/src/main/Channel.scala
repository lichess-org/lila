package lila.socket

import actorApi.SocketLeave
import akka.actor.ActorSystem
import play.api.libs.json.JsValue

import lila.hub.Trouper

final class Channel(system: ActorSystem) extends Trouper {

  system.lilaBus.subscribe(this, 'socketDoor)

  import Channel._

  private val members = scala.collection.mutable.Set.empty[SocketMember]

  val process: Trouper.Receive = {

    case Sub(member) => members += member

    case UnSub(member) => members -= member

    case SocketLeave(_, member) => members -= member

    case Publish(msg) => members.foreach(_ push msg)
  }
}

object Channel {

  case class Sub(member: SocketMember)
  case class UnSub(member: SocketMember)

  case class Publish(msg: JsValue)
}
