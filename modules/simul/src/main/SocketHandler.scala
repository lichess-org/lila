package lidraughts.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lidraughts.chat.Chat
import lidraughts.common.ApiVersion
import lidraughts.hub.actorApi.map._
import lidraughts.hub.Trouper
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User

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
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[Option[JsSocketHandler]] =
    exists(simulId) flatMap {
      _ ?? {
        val socket = socketMap getOrMake simulId
        socket.ask[Connected](Join(uid, user, version, _)) map {
          case Connected(enum, member) => Handler.iteratee(
            hub,
            lidraughts.chat.Socket.in(
              chatId = Chat.Id(simulId),
              member = member,
              chat = chat,
              publicSource = lidraughts.hub.actorApi.shutup.PublicSource.Simul(simulId).some
            ),
            member,
            socket,
            uid,
            apiVersion
          ) -> enum
        } map some
      }
    }
}
