package lila.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lila.chat.Chat
import lila.hub.actorApi.map._
import lila.security.Flood
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Uid, SocketVersion }
import lila.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: SocketHub,
    chat: ActorSelection,
    flood: Flood
) {

  def join(
    tourId: String,
    uid: Uid,
    user: Option[User],
    version: Option[SocketVersion]
  ): Fu[Option[JsSocketHandler]] =
    TournamentRepo.exists(tourId) flatMap {
      _ ?? {
        val socket = socketHub getOrMake tourId
        val join = Join(uid, user, version)
        Handler(hub, socket, uid, join) {
          case Connected(enum, member) =>
            (controller(socket, tourId, uid, member), enum, member)
        }.some
      }
    }

  private def controller(
    socket: ActorRef,
    tourId: String,
    uid: Uid,
    member: Member
  ): Handler.Controller = ({
    case ("p", o) => socket ! Ping(uid, o)
  }: Handler.Controller) orElse lila.chat.Socket.in(
    chatId = Chat.Id(tourId),
    member = member,
    socket = socket,
    chat = chat,
    publicSource = lila.hub.actorApi.shutup.PublicSource.Tournament(tourId).some
  )
}
