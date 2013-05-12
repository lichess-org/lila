package lila.tournament

import lila.socket.{ History, SocketHubActor }
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import actorApi._
import makeTimeout.short

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
    tournamentSocketName: String ⇒ String) extends SocketHubActor {

  def receiveSpecific = {

    case CloseTournament(id) ⇒ sockets get id foreach { socket ⇒
      socket ! Close
      sockets = sockets - id
    }

    case msg @ Fen(_, _, _) ⇒ broadcast(msg)
  }

  def mkSocket(tournamentId: String): ActorRef =
    context.actorOf(Props(new Socket(
      tournamentId = tournamentId,
      history = makeHistory(),
      messenger = messenger,
      uidTimeout = uidTimeout,
      socketTimeout = socketTimeout,
      getUsername = getUsername
    )), name = tournamentSocketName(tournamentId))
}
