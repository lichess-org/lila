package lila.tournament

import scala.concurrent.duration.Duration

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

import actorApi._
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.{ History, SocketHubActor }
import makeTimeout.short

private[tournament] final class SocketHub(
    makeHistory: () ⇒ History,
    messenger: Messenger,
    uidTimeout: Duration,
    socketTimeout: Duration,
    getUsername: String ⇒ Fu[Option[String]],
    tournamentSocketName: String ⇒ String) extends SocketHubActor {

  def receiveSpecific = {

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
