package lila.socket

import scala.concurrent.duration._

import akka.actor._
import akka.dispatch.Dispatchers
import akka.pattern.{ ask, pipe }
import akka.routing._

import actorApi._

final class SocketHub extends Actor {

  private val sockets = collection.mutable.Set[ActorRef]()

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'deploy, 'socket)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  import SocketHub._

  def receive = {

    case Open(socket)  => sockets += socket

    case Close(socket) => sockets -= socket

    case msg           => sockets foreach (_ ! msg)
  }
}

case object SocketHub {

  case class Open(actor: ActorRef)
  case class Close(actor: ActorRef)
}
