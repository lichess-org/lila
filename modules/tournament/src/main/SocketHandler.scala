package lila.tournament

import actorApi._
import lila.user.User
import lila.game.Game
import lila.socket.Handler
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.security.Flood
import lila.common.PimpedJson._
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.iteratee._

private[tournament] final class SocketHandler(
    socketHub: ActorRef,
    messenger: Messenger,
    flood: Flood) {

  def join(
    tour: Tournament,
    version: Int,
    uid: String,
    user: Option[User]): Fu[JsSocketHandler] = for {
    socket ← socketHub ? GetSocket(tour.id) mapTo manifest[ActorRef]
    join = Join(
      uid = uid,
      user = user,
      version = version)
    handler ← Handler(socket, uid, join) {
      case Connected(enum, member) ⇒
        controller(socket, tour.id, uid, member) -> enum
    }
  } yield handler

  private def controller(
    socket: ActorRef,
    tourId: String,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
    case ("liveGames", o) ⇒ o str "d" foreach { ids ⇒
      socket ! LiveGames(uid, ids.split(' ').toList)
    }
    case ("talk", o) ⇒ for {
      txt ← o str "d"
      if member.canChat
      userId ← member.userId
      if flood.allowMessage(uid, txt)
    } messenger.userMessage(tourId, userId, txt) pipeTo socket
    case _ ⇒
  }
}
