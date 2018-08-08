package lidraughts.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lidraughts.hub.actorApi.map._
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.Uid
import lidraughts.user.User
import lidraughts.chat.Chat
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    exists: Simul.ID => Fu[Boolean]
) {

  def join(
    simId: String,
    uid: Uid,
    user: Option[User]
  ): Fu[Option[JsSocketHandler]] =
    exists(simId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? Get(simId) mapTo manifest[ActorRef]
          join = Join(uid = uid, user = user)
          handler ← Handler(hub, socket, uid, join) {
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
  }: Handler.Controller) orElse lidraughts.chat.Socket.in(
    chatId = Chat.Id(simId),
    member = member,
    socket = socket,
    chat = chat,
    publicSource = lidraughts.hub.actorApi.shutup.PublicSource.Simul(simId).some
  )
}
