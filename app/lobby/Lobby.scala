package lila
package lobby

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scalaz.effects._

final class Lobby(hub: ActorRef) {

  implicit val timeout = Timeout(1 second)

  def join(uid: String): Promise[(Iteratee[JsValue, _], Enumerator[JsValue])] =
    (hub ? Join(uid)).asPromise.map {
      case Connected(enumerator) ⇒
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          (event \ "t").as[String] match {
            case "talk" ⇒ hub ! Talk(
              (event \ "data" \ "txt").as[String],
              (event \ "data" \ "u").as[String]
            )
          }
        }.mapDone { _ ⇒ hub ! Quit(uid) }
        (iteratee, enumerator)
    }

  def addEntry(entry: model.Entry): IO[Unit] = io {
    hub ! Entry(entry)
  }
}
