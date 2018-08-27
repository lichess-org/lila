package lila.socket

import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

import lila.hub.ActorMap

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap {

  override val supervisorStrategy =
    OneForOneStrategy() {
      // usually better to keep socket actors alive
      case _: Exception => Resume
    }

  def socketHubReceive: Receive = actorMapReceive
}

object SocketHubActor {

  trait Default[A <: SocketActor[_]] extends SocketHubActor[A] {

    def receive = socketHubReceive
  }
}
