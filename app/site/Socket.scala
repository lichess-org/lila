package lila
package site

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scalaz.effects._

final class Socket(hub: ActorRef) {

  implicit val timeout = Timeout(1 second)

  def join(uid: String, username: Option[String]): SocketPromise =
    (hub ? Join(uid, username)).asPromise map {
      case Connected(channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          Unit
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, channel)
    }
}
