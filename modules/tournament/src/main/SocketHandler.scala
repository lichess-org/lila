package lila.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi._
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection)(implicit system: ActorSystem) {

  def join(tourId: String, uid: String, user: Option[User]): Fu[Option[JsFlow]] =
    TournamentRepo.exists(tourId) flatMap {
      _ ?? {
        socketHub ? Get(tourId) mapTo manifest[ActorRef] map { socket =>
          Handler.actorRef { out =>
            val member = Member(out, user)
            socket ! AddMember(uid, member)
            Handler.props(hub, socket, member, uid, user.map(_.id)) {
              case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
              case ("talk", o) => o str "d" foreach { text =>
                member.userId foreach { userId =>
                  chat ! lila.chat.actorApi.UserTalk(tourId, userId, text, socket)
                }
              }
            }
          }.some
        }
      }
    }
}
