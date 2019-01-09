package lila.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lila.chat.Chat
import lila.common.ApiVersion
import lila.hub.actorApi.map._
import lila.hub.Trouper
import lila.security.Flood
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket
import lila.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lila.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    flood: Flood
) {

  def join(
    tourId: String,
    uid: Socket.Uid,
    user: Option[User],
    version: Option[Socket.SocketVersion],
    apiVersion: ApiVersion
  ): Fu[Option[JsSocketHandler]] =
    TournamentRepo exists tourId flatMap {
      _ ?? {
        val socket = socketMap getOrMake tourId
        socket.ask[Connected](Join(uid, user, version, _)) map {
          case Connected(enum, member) => Handler.iteratee(
            hub,
            lila.chat.Socket.in(
              chatId = Chat.Id(tourId),
              member = member,
              chat = chat,
              publicSource = lila.hub.actorApi.shutup.PublicSource.Tournament(tourId).some
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
