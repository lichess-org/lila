package lidraughts.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lidraughts.chat.Chat
import lidraughts.hub.actorApi.map._
import lidraughts.hub.Trouper
import lidraughts.security.Flood
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    flood: Flood
) {

  def join(
    tourId: String,
    uid: Uid,
    user: Option[User],
    version: Option[SocketVersion]
  ): Fu[Option[JsSocketHandler]] =
    TournamentRepo exists tourId flatMap {
      _ ?? {
        val socket = socketMap getOrMake tourId
        socket.ask[Connected](Join(uid, user, version, _)) map {
          case Connected(enum, member) => Handler.iteratee(
            hub,
            controller(socket, tourId, uid, member),
            member,
            socket,
            uid
          ) -> enum
        } map some
      }
    }

  private def controller(
    socket: Trouper,
    tourId: String,
    uid: Uid,
    member: Member
  ): Handler.Controller = ({
    case ("p", o) => socket ! Ping(uid, o)
  }: Handler.Controller) orElse lidraughts.chat.Socket.in(
    chatId = Chat.Id(tourId),
    member = member,
    chat = chat,
    publicSource = lidraughts.hub.actorApi.shutup.PublicSource.Tournament(tourId).some
  )
}
