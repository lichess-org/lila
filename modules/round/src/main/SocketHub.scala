package lila.round

import actorApi._
import lila.socket.actorApi._

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import makeTimeout.short

private[round] final class SocketHub(
    makeHistory: () ⇒ History,
    uidTimeout: Duration,
    socketTimeout: Duration,
    playerTimeout: Duration,
    gameSocketName: String ⇒ String) extends Actor {

  var sockets = Map.empty[String, ActorRef]

  def receive = {

    case Broom                            ⇒ broadcast(Broom)

    case msg @ SendTo(_, _)               ⇒ sockets.values foreach (_ ! msg)

    case msg @ SendTos(_, _)              ⇒ sockets.values foreach (_ ! msg)

    case msg @ GameEvents(gameId, events) ⇒ sockets get gameId foreach (_ forward msg)

    case GetSocket(id) ⇒ sender ! {
      (sockets get id) | {
        mkSocket(id) ~ { h ⇒ sockets = sockets + (id -> h) }
      }
    }

    case msg @ GetGameVersion(gameId) ⇒ (sockets get gameId).fold(sender ! 0) {
      _ ? msg pipeTo sender
    }

    case msg @ AnalysisAvailable(gameId) ⇒
      sockets get gameId foreach (_ forward msg)

    case CloseGame(gameId) ⇒ sockets get gameId foreach { socket ⇒
      socket ! Close
      sockets = sockets - gameId
    }

    case msg @ IsConnectedOnGame(gameId, color) ⇒ (sockets get gameId).fold(sender ! false) {
      _ ? msg pipeTo sender
    }

    case msg @ IsGone(gameId, color) ⇒ (sockets get gameId).fold(sender ! false) {
      _ ? msg pipeTo sender
    }

    case GetNbSockets ⇒ sender ! sockets.size

    case GetNbMembers ⇒ Future.traverse(sockets.values) { socket ⇒
      (socket ? GetNbMembers).mapTo[Int]
    } map (_.sum) pipeTo sender

    case GetUserIds ⇒ Future.traverse(sockets.values) { socket ⇒
      (socket ? GetUserIds).mapTo[Iterable[String]]
    } map (_.flatten) pipeTo sender

    case msg @ NbMembers(_) ⇒ sockets.values foreach (_ ! msg)
  }

  private def broadcast(msg: Any) {
    sockets.values foreach (_ ! msg)
  }

  private def mkSocket(id: String): ActorRef = context.actorOf(Props(new Socket(
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
