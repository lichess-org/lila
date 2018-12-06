package lidraughts.tournament

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lidraughts.chat.Chat
import lidraughts.hub.Trouper
import lidraughts.hub.actorApi.map._
import lidraughts.security.Flood
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import makeTimeout.short

private[tournament] final class SocketHandler(
    hub: lidraughts.hub.Env,
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
    TournamentRepo exists tourId flatMap {
      _ ?? {
        val socket = socketHub getOrMake tourId
        val promise = scala.concurrent.Promise[Connected]
        val join = JoinP(uid, user, version, promise)
        Handler.forTrouper(hub, socket, uid, join) {
          case Connected(enum, member) =>
            (controller(socket, tourId, uid, member), enum, member)
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
