package lila
package site

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import scalaz.effects._

import RichJs._
import socket.Util

final class Socket(hub: ActorRef) {

  implicit val timeout = Timeout(300 millis)

  def join(
    uidOption: Option[String],
    username: Option[String]): SocketPromise = {
    val promise: Option[SocketPromise] = for {
      uid ← uidOption
    } yield (hub ? Join(uid, username)).asPromise map {
      case Connected(channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { e ⇒
          e str "t" match {
            case Some("p") ⇒ {
              channel push Util.pong
            }
            case _ ⇒
          }
          Unit
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, channel)
    }
    promise | Util.connectionFail
  }
}
