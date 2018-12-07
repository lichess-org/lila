package lila.site

import actorApi._
import lila.socket._
import lila.socket.actorApi.StartWatching
import ornicar.scalalib.Random

private[site] final class SocketHandler(
    socket: Socket,
    hub: lila.hub.Env
) {

  def human(
    uid: Socket.Uid,
    userId: Option[String],
    flag: Option[String]
  ): Fu[JsSocketHandler] =
    socket.ask[Connected](JoinP(uid, userId, flag, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        PartialFunction.empty,
        member,
        socket,
        uid
      ) -> enum
    }

  def api: Fu[JsSocketHandler] = {

    val uid = Socket.Uid(Random secureString 8)
    val userId = none[String]
    val flag = "api".some

    def controller(member: SocketMember): Handler.Controller = {
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case _ => // not available on API socket
    }

    socket.ask[Connected](JoinP(uid, userId, flag, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller(member),
        member,
        socket,
        uid
      ) -> enum
    }
  }
}
