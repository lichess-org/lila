package lidraughts.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lidraughts.chat.Chat
import lidraughts.hub.actorApi.map._
import lidraughts.security.Flood
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.Uid
import lidraughts.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    flood: Flood
) {

  def join(
    tourId: String,
    uid: Uid,
    user: Option[User]
  ): Fu[Option[JsSocketHandler]] =
    TournamentRepo.exists(tourId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? Get(tourId) mapTo manifest[ActorRef]
          join = Join(uid, user = user)
          handler ← Handler(hub, socket, uid, join) {
            case Connected(enum, member) =>
              (controller(socket, tourId, uid, member), enum, member)
          }
        } yield handler.some
      }
    }

  private def controller(
    socket: ActorRef,
    tourId: String,
    uid: Uid,
    member: Member
  ): Handler.Controller = ({
    case ("p", o) => socket ! Ping(uid, o)
  }: Handler.Controller) orElse lidraughts.chat.Socket.in(
    chatId = Chat.Id(tourId),
    member = member,
    socket = socket,
    chat = chat,
    publicSource = lidraughts.hub.actorApi.shutup.PublicSource.Tournament(tourId).some
  )
}
