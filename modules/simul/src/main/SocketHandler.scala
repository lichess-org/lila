package lila.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.hub.actorApi.map._
import lila.hub.Trouper
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.user.User
import lila.chat.Chat
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lila.hub.Env,
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
  }: Handler.Controller) orElse lila.chat.Socket.in(
    chatId = Chat.Id(simulId),
    member = member,
    chat = chat,
    publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simulId).some
  )
}
