package lila
package round

import socket.{ Broom, Close, GetNbMembers, GetUsernames, NbMembers, SendTo, SendTos }

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.Future
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.Play.current

final class HubMaster(
    makeHistory: () ⇒ History,
    uidTimeout: Int,
    hubTimeout: Int,
    playerTimeout: Int) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher

  var hubs = Map.empty[String, ActorRef]

  def receive = {

    case Broom                            ⇒ hubs.values foreach (_ ! Broom)

    case msg @ SendTo(_, _)               ⇒ hubs.values foreach (_ ! msg)

    case msg @ SendTos(_, _)              ⇒ hubs.values foreach (_ ! msg)

    case msg @ GameEvents(gameId, events) ⇒ hubs get gameId foreach (_ forward msg)

    case GetHub(gameId: String) ⇒ sender ! {
      (hubs get gameId) | {
        mkHub(gameId) ~ { h ⇒ hubs = hubs + (gameId -> h) }
      }
    }

    case msg @ GetGameVersion(gameId) ⇒ (hubs get gameId).fold(
      _ ? msg pipeTo sender,
      sender ! 0
    )

    case msg @ AnalysisAvailable(gameId) ⇒
      hubs get gameId foreach (_ forward msg)

    case CloseGame(gameId) ⇒ hubs get gameId foreach { hub ⇒
      hub ! Close
      hubs = hubs - gameId
    }

    case msg @ IsConnectedOnGame(gameId, color) ⇒ (hubs get gameId).fold(
      _ ? msg pipeTo sender, sender ! false)

    case msg @ IsGone(gameId, color) ⇒ (hubs get gameId).fold(
      _ ? msg pipeTo sender, sender ! false)

    case GetNbHubs ⇒ sender ! hubs.size

    case GetNbMembers ⇒ Future.traverse(hubs.values) { hub ⇒
      (hub ? GetNbMembers).mapTo[Int]
    } map (_.sum) pipeTo sender

    case GetUsernames ⇒ Future.traverse(hubs.values) { hub ⇒
      (hub ? GetUsernames).mapTo[Iterable[String]]
    } map (_.flatten) pipeTo sender

    case msg @ NbMembers(_) ⇒ hubs.values foreach (_ ! msg)
  }

  private def mkHub(gameId: String): ActorRef = context.actorOf(Props(new Hub(
    gameId = gameId,
    history = makeHistory(),
    uidTimeout = uidTimeout,
    hubTimeout = hubTimeout,
    playerTimeout = playerTimeout
  )), name = "game_hub_" + gameId)
}
