package lila.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.user.User
import lila.chat.Chat
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    exists: Simul.ID => Fu[Boolean]
) {

  def join(
    simId: String,
    uid: Uid,
    user: Option[User],
    version: Option[SocketVersion]
  ): Fu[Option[JsSocketHandler]] =
    exists(simId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? Get(simId) mapTo manifest[ActorRef]
          join = Join(uid = uid, user = user, version = version)
          handler ← Handler.forActor(hub, socket, uid, join) {
            case Connected(enum, member) =>
              (controller(socket, simId, uid, member), enum, member)
          }
        } yield handler.some
      }
    }

  private def controller(
    socket: ActorRef,
    simId: String,
    uid: Uid,
    member: Member
  ): Handler.Controller = ({
    case ("p", o) => socket ! Ping(uid, o)
  }: Handler.Controller) orElse lila.chat.Socket.in(
    chatId = Chat.Id(simId),
    member = member,
    chat = chat,
    publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simId).some
  )
}
