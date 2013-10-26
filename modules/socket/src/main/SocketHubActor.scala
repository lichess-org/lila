package lila.socket

import akka.actor.{ Deploy ⇒ _, _ }
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import play.api.libs.json._

import actorApi._
import lila.hub.actorApi.{ GetNbMembers, NbMembers, WithUserIds, WithSocketUserIds, SendTo, SendTos, Deploy }
import lila.hub.ActorMap
import lila.socket.actorApi.{ Connected ⇒ _, _ }

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap[A] {

  def socketHubReceive: Receive = _socketHubReceive orElse actorMapReceive

  private def _socketHubReceive: Receive = {

    case WithSocketUserIds(id, f) ⇒ withActor(id) { _ ! WithUserIds(f) }

    case msg: GetNbMembers.type   ⇒ zipAll[Int](msg) pipeTo sender

    case msg: NbMembers           ⇒ tellAll(msg)

    case msg: WithUserIds         ⇒ tellAll(msg)

    case msg: Broom.type          ⇒ tellAll(msg)

    case msg: SendTo              ⇒ tellAll(msg)

    case msg: SendTos             ⇒ tellAll(msg)

    case msg: Deploy              ⇒ tellAll(msg)
  }
}

object SocketHubActor {

  trait Default[A <: SocketActor[_]] extends SocketHubActor[A] {

    def receive = socketHubReceive
  }
}
