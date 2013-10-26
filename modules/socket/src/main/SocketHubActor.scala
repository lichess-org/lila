package lila.socket

import akka.actor._

import lila.hub.ActorMap

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap[A] {

  def socketHubReceive: Receive = actorMapReceive
}

object SocketHubActor {

  trait Default[A <: SocketActor[_]] extends SocketHubActor[A] {

    def receive = socketHubReceive
  }
}
