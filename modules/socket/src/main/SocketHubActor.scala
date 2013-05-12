package lila.socket

import actorApi._
import lila.hub.actorApi.{ GetNbMembers, NbMembers, GetUserIds, WithUserIds, WithSocketUserIds }
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

abstract class SocketHubActor extends Actor {

  def mkSocket(id: String): ActorRef

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case GetNbSockets ⇒ sender ! sockets.size

    case GetNbMembers ⇒ {
      sockets.values.toList map (_ ? GetNbMembers mapTo manifest[Int])
    }.sequence map (_.sum) pipeTo sender

    case msg @ NbMembers(_) ⇒ broadcast(msg)

    case WithSocketUserIds(id, f) ⇒ withSocket(id) { _ ! WithUserIds(f) }

    case GetUserIds ⇒ {
      sockets.values.toList map (_ ? GetUserIds mapTo manifest[Iterable[String]])
    }.sequence map (_.flatten) pipeTo sender

    case Broom               ⇒ broadcast(Broom)

    case msg @ SendTo(_, _)  ⇒ broadcast(msg)

    case msg @ SendTos(_, _) ⇒ broadcast(msg)

    case Forward(id, GetVersion) ⇒ (sockets get id).fold(sender ! 0) {
      _ ? GetVersion pipeTo sender
    }

    case Forward(id, msg)    ⇒ withSocket(id)(_ forward msg)

    case GetSocket(id: String) ⇒ sender ! {
      (sockets get id) | {
        mkSocket(id) ~ { s ⇒ sockets = sockets + (id -> s) }
      }
    }

    case CloseSocket(id) ⇒ withSocket(id) { socket ⇒
      socket ! Close
      sockets = sockets - id
    }
  }

  def receive = receiveSpecific orElse receiveGeneric

  var sockets = Map.empty[String, ActorRef]

  def withSocket(id: String)(f: ActorRef => Unit) = 
    sockets get id foreach f

  def broadcast(msg: Any) {
    sockets.values foreach (_ ! msg)
  }
}
