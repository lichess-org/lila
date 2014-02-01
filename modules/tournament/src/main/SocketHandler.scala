package lila.tournament

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.db.api.$count
import lila.game.Game
import lila.hub.actorApi.map._
import lila.security.Flood
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short
import tube.tournamentTube

private[tournament] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    flood: Flood) {

  def join(
    tourId: String,
    version: Int,
    uid: String,
    user: Option[User]): Fu[JsSocketHandler] =
    $count.exists(tourId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? Get(tourId) mapTo manifest[ActorRef]
          join = Join(
            uid = uid,
            user = user,
            version = version)
          handler ← Handler(hub, socket, uid, join, user map (_.id)) {
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
      t ← o str "d"
      userId ← member.userId
      if flood.allowMessage(uid, t)
    } socket ! Talk(tourId, userId, t, member.troll)
  }
}
