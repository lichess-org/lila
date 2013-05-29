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
    getUsername: String ⇒ Fu[Option[String]]) extends SocketHubActor[Socket] {

  def receive = PartialFunction[Any, Unit]({

    case msg: Fen ⇒ tellAll(msg)

  }) orElse socketHubReceive

  def mkActor(tournamentId: String) = new Socket(
      tournamentId = tournamentId,
      history = makeHistory(),
      messenger = messenger,
      uidTimeout = uidTimeout,
      socketTimeout = socketTimeout,
      getUsername = getUsername)
}
