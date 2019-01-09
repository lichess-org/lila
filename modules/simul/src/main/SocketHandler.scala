package lila.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.chat.Chat
import lila.common.ApiVersion
import lila.hub.actorApi.map._
import lila.hub.Trouper
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.user.User

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
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[Option[JsSocketHandler]] =
    exists(simulId) flatMap {
      _ ?? {
        val socket = socketMap getOrMake simulId
        socket.ask[Connected](Join(uid, user, version, _)) map {
          case Connected(enum, member) => Handler.iteratee(
            hub,
            lila.chat.Socket.in(
              chatId = Chat.Id(simulId),
              member = member,
              chat = chat,
              publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simulId).some
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
