package lila
package round

import socket.Fen

import scalaz.effects._
import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.Play.current

final class MoveNotifier(
  siteHubName: String,
  countMove: () => Unit) {

  lazy val siteHubRef = Akka.system.actorFor("/user/" + siteHubName)

  def apply(gameId: String, fen: String): IO[Unit] = io {
    countMove()
    siteHubRef ! Fen(gameId, fen)
  }
}
