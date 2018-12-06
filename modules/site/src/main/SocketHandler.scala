package lidraughts.site

import actorApi._
import lidraughts.socket._

private[site] final class SocketHandler(
    socket: akka.actor.ActorRef,
    hub: lidraughts.hub.Env
) {
  def apply(
    uid: Socket.Uid,
    userId: Option[String],
    apiVersion: Int,
    flag: Option[String]
  ): Fu[JsSocketHandler] =
    Handler.forActor(hub, socket, uid, Join(uid, userId, flag)) {
      case Connected(enum, member) => (Handler.emptyController, enum, member)
    }
}
