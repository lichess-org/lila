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
    hubNames: List[String],
    countMove: () ⇒ Unit) {

  lazy val hubRefs = hubNames map { name ⇒ Akka.system.actorFor("/user/" + name) }

  def apply(gameId: String, fen: String, lastMove: Option[String]) {
    countMove()
    val message = Fen(gameId, fen, lastMove)
    hubRefs foreach (_ ! message)
  }
}
