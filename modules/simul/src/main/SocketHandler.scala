package lidraughts.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lidraughts.chat.Chat
import lidraughts.hub.actorApi.map._
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketHub: lidraughts.hub.ActorMapNew,
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
        val socket = socketHub getOrMake simId
        val join = Join(uid = uid, user = user, version = version)
        Handler(hub, socket, uid, join) {
          case Connected(enum, member) =>
            (controller(socket, simId, uid, member), enum, member)
        } map some
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
