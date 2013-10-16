package lila.tournament

import scala.concurrent.duration.Duration

import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import play.api.libs.json._

import actorApi._
import lila.hub.actorApi.round.MoveEvent
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.{ History, SocketHubActor }

private[tournament] final class SocketHub(
    makeHistory: () ⇒ History,
    messenger: Messenger,
    uidTimeout: Duration,
    socketTimeout: Duration,
    getUsername: String ⇒ Fu[Option[String]]) extends SocketHubActor[Socket] {

  def receive: Receive = _receive orElse socketHubReceive

  private def _receive: Receive = {

    case msg: MoveEvent ⇒ tellAll(msg)
  }

  def mkActor(tournamentId: String) = new Socket(
    tournamentId = tournamentId,
    history = makeHistory(),
    messenger = messenger,
    uidTimeout = uidTimeout,
    socketTimeout = socketTimeout,
    getUsername = getUsername)
}
