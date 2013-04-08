package lila.site

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.socket._
import lila.socket.actorApi.LiveGames

private[site] final class SocketHandler(socket: ActorRef) {

  def join(
    uid: String,
    userId: Option[String],
    flag: Option[String]): Fu[JsSocketHandler] =
    Handler(socket, uid, Join(uid, userId, flag)) {
      case ("liveGames", o) ⇒ o str "d" foreach { ids ⇒
        socket ! LiveGames(uid, ids.split(' ').toList)
      }
    }
}
