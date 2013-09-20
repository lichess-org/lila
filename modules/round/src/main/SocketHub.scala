package lila.round

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

import actorApi._
import lila.socket.actorApi._
import lila.socket.SocketHubActor
import makeTimeout.short

private[round] final class SocketHub(
    makeHistory: () ⇒ History,
    getUsername: String ⇒ Fu[Option[String]],
    uidTimeout: Duration,
    socketTimeout: Duration,
    disconnectTimeout: Duration,
    ragequitTimeout: Duration) extends SocketHubActor[Socket] {

  def receive: Receive = _receive orElse socketHubReceive

  def _receive: Receive = {

    case lila.game.actorApi.ChangeFeaturedGame(game) ⇒ tellAll {
      lila.game.actorApi.TellWatchers(makeMessage("featured_id", game.id))
    }
  }

  def mkActor(id: String) = new Socket(
    gameId = id,
    history = makeHistory(),
    getUsername = getUsername,
    uidTimeout = uidTimeout,
    socketTimeout = socketTimeout,
    disconnectTimeout = disconnectTimeout,
    ragequitTimeout = ragequitTimeout)
}
