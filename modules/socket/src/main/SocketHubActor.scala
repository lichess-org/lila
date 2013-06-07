package lila.socket

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

import actorApi._
import lila.hub.actorApi.{ GetNbMembers, NbMembers, WithUserIds, WithSocketUserIds, SendTo, SendTos }
import lila.hub.ActorMap
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import makeTimeout.short

trait SocketHubActor[A <: SocketActor[_]] extends ActorMap[A] {

  def socketHubReceive: Receive = _socketHubReceive orElse actorMapReceive

  private def _socketHubReceive: Receive = {

    case msg @ GetNbMembers ⇒
      askAll(msg) mapTo manifest[List[Int]] map (_.sum) pipeTo sender

    case msg @ NbMembers(_)       ⇒ tellAll(msg)

    case WithSocketUserIds(id, f) ⇒ withActor(id) { _ ! WithUserIds(f) }

    case msg @ WithUserIds(_)     ⇒ tellAll(msg)

    case msg @ Broom              ⇒ tellAll(msg)

    case msg @ SendTo(_, _)       ⇒ tellAll(msg)

    case msg @ SendTos(_, _)      ⇒ tellAll(msg)

    case msg: Deploy              ⇒ tellAll(msg)
  }
}
