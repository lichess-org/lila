package lila.tournament

import lila.socket.History
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.hub.actorApi.{ GetNbMembers, NbMembers, GetUserIds }
import actorApi._
import makeTimeout.short

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

private[tournament] final class SocketHub(
    makeHistory: () ⇒ History,
    messenger: Messenger,
    uidTimeout: Duration,
    socketTimeout: Duration,
    getUsername: String ⇒ Fu[Option[String]],
    tournamentSocketName: String ⇒ String) extends Actor {

  var sockets = Map.empty[String, ActorRef]

  def receive = {

    case Broom               ⇒ broadcast(Broom)

    case msg @ SendTo(_, _)  ⇒ broadcast(msg)

    case msg @ SendTos(_, _) ⇒ broadcast(msg)

    case Forward(id, msg)    ⇒ sockets get id foreach (_ ! msg)

    case GetSocket(id: String) ⇒ sender ! {
      (sockets get id) | {
        mkSocket(id) ~ { s ⇒ sockets = sockets + (id -> s) }
      }
    }

    case msg @ GetTournamentVersion(id) ⇒ (sockets get id).fold(sender ! 0) {
      _ ? msg pipeTo sender
    }

    case CloseTournament(id) ⇒ sockets get id foreach { socket ⇒
      socket ! Close
      sockets = sockets - id
    }

    case GetNbSockets ⇒ sender ! sockets.size

    case GetNbMembers ⇒ Future.traverse(sockets.values) { socket ⇒
      (socket ? GetNbMembers).mapTo[Int]
    } map (_.sum) pipeTo sender

    case GetTournamentUserIds(id) ⇒ (sockets get id).fold(sender ! Nil) { socket ⇒
      (socket ? GetUserIds).mapTo[Iterable[String]] pipeTo sender
    }

    case GetTournamentIds ⇒ sockets.keys

    case GetUserIds ⇒ Future.traverse(sockets.values) { socket ⇒
      (socket ? GetUserIds).mapTo[Iterable[String]]
    } map (_.flatten) pipeTo sender

    case msg @ NbMembers(_) ⇒ broadcast(msg)

    case msg @ Fen(_, _, _) ⇒ broadcast(msg)
  }

  private def broadcast(msg: Any) {
    sockets.values foreach (_ ! msg)
  }

  private def mkSocket(tournamentId: String): ActorRef =
    context.actorOf(Props(new Socket(
      tournamentId = tournamentId,
      history = makeHistory(),
      messenger = messenger,
      uidTimeout = uidTimeout,
      socketTimeout = socketTimeout,
      getUsername = getUsername
    )), name = tournamentSocketName(tournamentId))
}
