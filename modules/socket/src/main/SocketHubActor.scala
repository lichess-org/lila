package lila.socket

import akka.actor._

import actorApi._
import lila.hub.ActorMap
import lila.socket.actorApi.{ Connected ⇒ _, _ }

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap[A] {

  def socketHubReceive: Receive = actorMapReceive
}

object SocketHubActor {

  trait Default[A <: SocketActor[_]] extends SocketHubActor[A] {

    def receive = socketHubReceive
  }
}
