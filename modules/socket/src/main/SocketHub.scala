package lila.socket

import akka.actor._

final class SocketHub extends Actor {

  private val sockets = collection.mutable.Set[ActorRef]()

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'deploy, 'socket)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  import SocketHub._

  def receive = {

    case Open(socket) => sockets += socket

    case Close(socket) => sockets -= socket

    case lila.hub.actorApi.DeployPre => // ignore

    case msg => sockets foreach (_ ! msg)
  }
}

case object SocketHub {

  case class Open(actor: ActorRef)
  case class Close(actor: ActorRef)
}
