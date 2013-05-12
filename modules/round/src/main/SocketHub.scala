package lila.round

import actorApi._
import lila.socket.actorApi._
import lila.socket.SocketHubActor
import lila.hub.actorApi.{ GetNbMembers, NbMembers, GetUserIds }

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.libs.json._
import makeTimeout.short

private[round] final class SocketHub(
    makeHistory: () ⇒ History,
    uidTimeout: Duration,
    socketTimeout: Duration,
    playerTimeout: Duration,
    gameSocketName: String ⇒ String) extends SocketHubActor {

  def receiveSpecific = {

    case CloseGame(gameId) ⇒ withSocket(gameId) { socket ⇒
      socket ! Close
      sockets = sockets - gameId
    }

    case msg @ IsConnectedOnGame(gameId, color) ⇒ (sockets get gameId).fold(sender ! false) {
      _ ? msg pipeTo sender
    }

    case msg @ IsGone(gameId, color) ⇒ (sockets get gameId).fold(sender ! false) {
      _ ? msg pipeTo sender
    }
  }

  def mkSocket(id: String): ActorRef = context.actorOf(Props(new Socket(
    gameId = id,
    history = context.actorOf(
      Props(makeHistory()),
      name = gameSocketName(id) + "-history"
    ),
    uidTimeout = uidTimeout,
    socketTimeout = socketTimeout,
    playerTimeout = playerTimeout
  )), name = gameSocketName(id))
}
