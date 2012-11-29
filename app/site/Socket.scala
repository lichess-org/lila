package lila
package site

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import scalaz.effects._

import implicits.RichJs._
import socket.{ Util, Ping, Quit, LiveGames }

final class Socket(hub: ActorRef) {

  implicit val timeout = Timeout(300 millis)

  def sendToFlag(flag: String, message: JsObject) {
    hub ! SendToFlag(flag, message)
  }

  def join(
    uidOption: Option[String],
    username: Option[String],
    flag: Option[String]): SocketPromise = {
    val promise: Option[SocketPromise] = for {
      uid ← uidOption
    } yield (hub ? Join(uid, username, flag)).asPromise map {
      case Connected(enumerator, channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { e ⇒
          e str "t" match {
            case Some("p") ⇒ hub ! Ping(uid)
            case Some("liveGames") ⇒ e str "d" foreach { ids ⇒
              hub ! LiveGames(uid, ids.split(' ').toList)
            }
            case _ ⇒
          }
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, enumerator)
    }
    promise | Util.connectionFail
  }
}
