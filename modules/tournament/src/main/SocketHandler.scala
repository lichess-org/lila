package lila.tournament

import actorApi._
import lila.user.User
import lila.game.Game
import lila.socket.Handler
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.security.Flood
import lila.common.PimpedJson._
import tube.tournamentTube
import lila.db.api.$count
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
    tourId: String,
    version: Int,
    uid: String,
    user: Option[User]): Fu[JsSocketHandler] =
    $count.exists(tourId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? GetSocket(tourId) mapTo manifest[ActorRef]
          join = Join(
            uid = uid,
            user = user,
            version = version)
          handler ← Handler(socket, uid, join) {
            case Connected(enum, member) ⇒
              controller(socket, tourId, uid, member) -> enum
          }
        } yield handler
      }
    }

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
    } socket ! Talk(tourId, userId, txt)
    case _ ⇒
  }
}
