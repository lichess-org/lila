package lila
package game

import model._
import socket.{ Broom, Close }

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import akka.event.Logging
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.Play.current

final class HubMaster(
    makeHistory: () ⇒ History,
    uidTimeout: Int,
    hubTimeout: Int) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher
  val log = Logging(context.system, this)

  var hubs = Map.empty[String, ActorRef]

  def receive = {

    case Broom                ⇒ hubs.values foreach (_ ! Broom)

    case Forward(gameId, msg) ⇒ hubs get gameId foreach (_ ! msg)

    case GetHub(gameId: String) ⇒ sender ! {
      (hubs get gameId) | {
        mkHub(gameId) ~ { h ⇒ hubs = hubs + (gameId -> h) }
      }
    }

    case msg @ GetGameVersion(gameId) => (hubs get gameId).fold(
      _ ? msg pipeTo sender,
      sender ! 0
    )

    case CloseGame(gameId) ⇒ hubs get gameId foreach { hub ⇒
      log.warning("close game " + gameId)
      hub ! Close
      hubs = hubs - gameId
    }

    case msg @ IsConnectedOnGame(gameId, color) ⇒ (hubs get gameId).fold(
      _ ? msg pipeTo sender,
      sender ! false
    )
  }

  private def mkHub(gameId: String): ActorRef = context.actorOf(Props(new Hub(
    gameId = gameId,
    history = makeHistory(),
    uidTimeout = uidTimeout,
    hubTimeout = hubTimeout
  )), name = "game_hub_" + gameId)
}
