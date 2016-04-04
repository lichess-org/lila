package lila.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi._
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    exists: Simul.ID => Fu[Boolean])(implicit val system: ActorSystem) {

  def join(
    simId: String,
    uid: String,
    user: Option[User]): Fu[Option[JsFlow]] = exists(simId) flatMap {
    _ ?? {
      socketHub ? Get(simId) mapTo manifest[ActorRef] map { socket =>
        Handler.actorRef { out =>
          val member = Member(out, user)
          socket ! AddMember(uid, member)
          Handler.props(hub, socket, member, uid, user.map(_.id)) {
            case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
            case ("talk", o) => o str "d" foreach { text =>
              member.userId foreach { userId =>
                chat ! lila.chat.actorApi.UserTalk(simId, userId, text, socket)
              }
            }
          }
        }.some
      }
    }
  }
}
