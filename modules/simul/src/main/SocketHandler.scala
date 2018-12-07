package lidraughts.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lidraughts.hub.actorApi.map._
import lidraughts.hub.Trouper
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import lidraughts.chat.Chat
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    exists: Simul.ID => Fu[Boolean]
) {

  def join(
    simulId: String,
    uid: Uid,
    user: Option[User],
    version: Option[SocketVersion]
  ): Fu[Option[JsSocketHandler]] =
    exists(simulId) flatMap {
      _ ?? {
        val socket = socketMap getOrMake simulId
        socket.ask[Connected](JoinP(uid, user, version, _)) map {
          case Connected(enum, member) => Handler.iteratee(
            hub,
            controller(socket, simulId, uid, member),
            member,
            socket,
            uid
          ) -> enum
        } map some
      }
    }

  private def controller(
    socket: Trouper,
    simulId: String,
    uid: Uid,
    member: Member
  ): Handler.Controller = ({
    case ("p", o) => socket ! Ping(uid, o)
  }: Handler.Controller) orElse lidraughts.chat.Socket.in(
    chatId = Chat.Id(simulId),
    member = member,
    chat = chat,
    publicSource = lidraughts.hub.actorApi.shutup.PublicSource.Simul(simulId).some
  )
}
