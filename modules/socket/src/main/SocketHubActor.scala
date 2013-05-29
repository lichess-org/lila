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

  def socketHubReceive: Receive = PartialFunction[Any, Unit]({

    case msg @ GetNbMembers ⇒ 
      askAll(msg) mapTo manifest[List[Int]] map (_.sum) pipeTo sender

    case msg @ NbMembers(_)       ⇒ tellAll(msg)

    case WithSocketUserIds(id, f) ⇒ withActor(id) { _ ! WithUserIds(f) }

    case msg @ WithUserIds(_)     ⇒ tellAll(msg)

    case Broom                    ⇒ tellAll(Broom)

    case msg @ SendTo(_, _)       ⇒ tellAll(msg)

    case msg @ SendTos(_, _)      ⇒ tellAll(msg)

  }) orElse actorMapReceive
}
