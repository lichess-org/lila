package lila.site

import actorApi._
import lila.socket._
import lila.socket.actorApi.StartWatching

import ornicar.scalalib.Random

private[site] final class ApiSocketHandler(
    socket: akka.actor.ActorRef,
    hub: lila.hub.Env
) {

  private val flag = "api".some
  private val userId = none[String]

  def apply: Fu[JsSocketHandler] = {

    val uid = Socket.Uid(Random secureString 8)

    def controller(member: SocketMember): Handler.Controller = {
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case _ => // not available on API socket
    }

    Handler.forActor(hub, socket, uid, Join(uid, userId, flag)) {
      case Connected(enum, member) => (controller(member), enum, member)
    }
  }
}
