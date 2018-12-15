package lila.site

import play.api.libs.json.JsNumber
import ornicar.scalalib.Random

import actorApi._
import lila.socket._
import lila.socket.actorApi.StartWatching
import lila.common.ApiVersion

private[site] final class SocketHandler(
    socket: Socket,
    hub: lila.hub.Env
) {

  def human(
    uid: Socket.Uid,
    userId: Option[String],
    apiVersion: ApiVersion,
    flag: Option[String]
  ): Fu[JsSocketHandler] =
    socket.ask[Connected](Join(uid, userId, flag, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller = PartialFunction.empty,
        member,
        socket,
        uid,
        apiVersion
      ) -> enum
    }

  def api(apiVersion: ApiVersion): Fu[JsSocketHandler] = {

    val uid = Socket.Uid(Random secureString 8)
    val userId = none[String]
    val flag = "api".some

    def controller(member: SocketMember): Handler.Controller = {
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.bus.publish(StartWatching(uid, member, ids.split(' ').toSet), 'socketMoveBroadcast)
      }
      case _ => // not available on API socket
    }

    socket.ask[Connected](Join(uid, userId, flag, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller(member),
        member,
        socket,
        uid,
        apiVersion
      ) -> enum
    }
  }
}
