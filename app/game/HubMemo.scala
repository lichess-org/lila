package lila
package game

import memo._
import model._
import socket._
import chess.Color

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor.{ ActorRef, Props, PoisonPill }

import scalaz.effects._
import scala.collection.JavaConversions._

final class HubMemo(
    makeHistory: () ⇒ History,
    timeout: Int) {

  private val cache = {
    import com.google.common.cache._
    import java.util.concurrent.TimeUnit
    import memo.Builder._
    CacheBuilder.newBuilder()
      .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
      .asInstanceOf[CacheBuilder[String, ActorRef]]
      .removalListener(onRemove _)
      .build[String, ActorRef](compute _)
  }

  def all: Map[String, ActorRef] = cache.asMap.toMap

  def get(gameId: String): ActorRef = cache get gameId

  def getFromFullId(fullId: String): ActorRef = get(DbGame takeGameId fullId)

  def shake(gameId: String): IO[Unit] = io {
    cache get gameId
    Unit
  }

  private def compute(gameId: String): ActorRef = {
    println("create actor game " + gameId)
    Akka.system.actorOf(Props(new Hub(gameId, makeHistory())))
  }

  private def onRemove(gameId: String, actor: ActorRef) {
    println("kill actor game " + gameId)
    actor ! Close
  }
}
