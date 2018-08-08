package lidraughts.socket

import akka.actor._

final class SocketHub extends Actor {

  private val sockets = collection.mutable.Set[ActorRef]()

  override def preStart(): Unit = {
    context.system.lidraughtsBus.subscribe(self, 'deploy, 'socket)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lidraughtsBus.unsubscribe(self)
  }

  import SocketHub._

  def receive = {

    case Open(socket) => sockets += socket

    case Close(socket) => sockets -= socket

    case msg => sockets foreach (_ ! msg)
  }
}

case object SocketHub {

  case class Open(actor: ActorRef)
  case class Close(actor: ActorRef)
}
