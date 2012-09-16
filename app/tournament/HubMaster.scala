package lila
package tournament

import socket.{ History, Broom, Close, GetNbMembers, GetUsernames, NbMembers, SendTo, Fen }

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.Play.current

final class HubMaster(
    makeHistory: () ⇒ History,
    messenger: Messenger,
    uidTimeout: Int,
    hubTimeout: Int) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher

  var hubs = Map.empty[String, ActorRef]

  def receive = {

    case Broom              ⇒ hubs.values foreach (_ ! Broom)

    case msg @ SendTo(_, _) ⇒ hubs.values foreach (_ ! msg)

    case Forward(id, msg)   ⇒ hubs.get(id).foreach(_ ! msg)

    case GetHub(id: String) ⇒ sender ! {
      (hubs get id) | {
        mkHub(id) ~ { h ⇒ hubs = hubs + (id -> h) }
      }
    }

    case msg @ GetTournamentVersion(id) ⇒ (hubs get id).fold(
      _ ? msg pipeTo sender,
      sender ! 0
    )

    case CloseTournament(id) ⇒ hubs get id foreach { hub ⇒
      hub ! Close
      hubs = hubs - id
    }

    case GetNbHubs ⇒ sender ! hubs.size

    case GetNbMembers ⇒ Future.traverse(hubs.values) { hub ⇒
      (hub ? GetNbMembers).mapTo[Int]
    } map (_.sum) pipeTo sender

    case GetTournamentUsernames(id) ⇒ hubs get id fold (
      hub ⇒ (hub ? GetUsernames).mapTo[Iterable[String]] pipeTo sender,
      sender ! Nil
    )

    case GetTournamentIds ⇒ hubs.keys

    case GetUsernames ⇒ Future.traverse(hubs.values) { hub ⇒
      (hub ? GetUsernames).mapTo[Iterable[String]]
    } map (_.flatten) pipeTo sender

    case msg @ NbMembers(_) ⇒ hubs.values foreach (_ ! msg)

    case msg @ Fen(_, _, _) ⇒ hubs.values foreach (_ ! msg)
  }

  private def mkHub(tournamentId: String): ActorRef =
    context.actorOf(Props(new Hub(
      tournamentId = tournamentId,
      messenger = messenger,
      history = makeHistory(),
      uidTimeout = uidTimeout,
      hubTimeout = hubTimeout
    )), name = "tournament_hub_" + tournamentId)
}
