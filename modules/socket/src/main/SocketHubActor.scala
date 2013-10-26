package lila.socket

import akka.actor.{ Deploy ⇒ _, _ }
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

import actorApi._
import lila.hub.actorApi.{ GetNbMembers, NbMembers, Deploy }
import lila.hub.ActorMap
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import makeTimeout.short

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap[A] {

  def socketHubReceive: Receive = _socketHubReceive orElse actorMapReceive

  private def _socketHubReceive: Receive = {

    case msg: GetNbMembers.type ⇒ zipAll[Int](msg) pipeTo sender
  }
}

object SocketHubActor {

  trait Default[A <: SocketActor[_]] extends SocketHubActor[A] {

    def receive = socketHubReceive
  }
}
