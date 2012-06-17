package lila
package round

import socket.Fen

import scalaz.effects._
import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.Play.current

final class FenNotifier(siteHubName: String) {

  lazy val siteHubRef = Akka.system.actorFor("/user/" + siteHubName)

  def apply(gameId: String, fen: String): IO[Unit] = io {
    siteHubRef ! Fen(gameId, fen)
  }
}
