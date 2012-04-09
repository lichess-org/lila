package lila
package game

import memo._
import model._
import socket.History
import chess.Color

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor.{ ActorRef, Props, PoisonPill }

import scalaz.effects._

final class HubMemo(
    makeHistory: () ⇒ History,
    timeout: Int) {

  private val cache = Builder.cacheWithRemovalListener(timeout, compute) {
    (uid, actor) ⇒ {
      println("kill actor " + uid)
      actor ! PoisonPill
    }
  }

  def get(gameId: String): ActorRef = cache get gameId

  def getFromFullId(fullId: String): ActorRef = get(DbGame takeGameId fullId)

  def put(gameId: String, actor: ActorRef) = cache.put(gameId, actor)

  private def compute(gameId: String): ActorRef =
    Akka.system.actorOf(Props(new Hub(gameId, makeHistory())))
}
