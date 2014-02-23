package lila.socket

import scala.concurrent.duration._

import akka.actor._
import akka.dispatch.Dispatchers
import akka.pattern.{ ask, pipe }
import akka.routing._

import actorApi._

final class SocketHub extends Actor {

  private val sockets = collection.mutable.Set[ActorRef]()

  context.system.lilaBus.subscribe(self, 'moveEvent, 'users, 'deploy, 'nbMembers, 'socket)

  override def postStop() {
    context.system.lilaBus.unsubscribe(self)
  }

  import SocketHub._

  def receive = {

    case Subscribe(socket)   => sockets += socket

    case Unsubscribe(socket) => sockets -= socket

    case msg                 => sockets foreach (_ ! msg)
  }
}

case object SocketHub {

  case class Subscribe(actor: ActorRef)
  case class Unsubscribe(actor: ActorRef)
}
