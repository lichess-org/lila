package lila.site

import scala.concurrent.duration._

import akka.actor._
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.socket._
import lila.socket.actorApi.{ Connected, LiveGames }

private[site] final class SocketHandler(
    socket: ActorRef,
    hub: lila.hub.Env) {

  def apply(
    uid: String,
    userId: Option[String],
    flag: Option[String]): Fu[JsSocketHandler] = {

    def controller: Handler.Controller = {
      case ("liveGames", o) => o str "d" foreach { ids =>
        socket ! LiveGames(uid, ids.split(' ').toList)
      }
    }

    Handler(hub, socket, uid, Join(uid, userId, flag), userId) {
      case Connected(enum, member) =>
        controller -> enum
    }
  }
}
