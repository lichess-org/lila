package lila
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid, username) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      addMember(uid, Member(channel, username))
      sender ! Connected(enumerator, channel)
    }

    case Fen(gameId, fen)   ⇒ notifyFen(gameId, fen)

    case LiveGames(uid, gameIds) ⇒ registerLiveGames(uid, gameIds)
  }

  def notifyFen(gameId: String, fen: String) {
    val msg = makeMessage("fen", JsObject(Seq(
      "id" -> JsString(gameId),
      "fen" -> JsString(fen))))
    members.values filter (_ liveGames gameId) foreach (_.channel push msg)
  }

  def registerLiveGames(uid: String, ids: List[String]) {
    member(uid) foreach (_ addLiveGames ids)
  }
}
