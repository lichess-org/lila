package lila.socket

import actorApi._
import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import play.api.libs.json._

import lila.hub.actorApi.{ GetNbMembers, NbMembers, WithUserIds, WithSocketUserIds, SendTo, SendTos }
import lila.hub.ActorMap
import lila.socket.actorApi.{ Connected ⇒ _, _ }

trait SocketHubActor[A <: SocketActor[_]] extends Socket with ActorMap[A] {

  def socketHubReceive: Receive = _socketHubReceive orElse actorMapReceive

  private def _socketHubReceive: Receive = {

    case msg @ GetNbMembers       ⇒ zipAll[Int](msg) pipeTo sender

    case msg @ NbMembers(_)       ⇒ tellAll(msg)

    case WithSocketUserIds(id, f) ⇒ withActor(id) { _ ! WithUserIds(f) }

    case msg @ WithUserIds(_)     ⇒ tellAll(msg)

    case msg @ Broom              ⇒ tellAll(msg)

    case msg @ SendTo(_, _)       ⇒ tellAll(msg)

    case msg @ SendTos(_, _)      ⇒ tellAll(msg)

    case msg: Deploy              ⇒ tellAll(msg)
  }
}
