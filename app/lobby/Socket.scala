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

final class Socket(hub: ActorRef) {

  implicit val timeout = Timeout(1 second)

  def join(
    uid: String,
    version: Int,
    username: Option[String],
    hook: Option[String]): SocketPromise =
    (hub ? Join(uid, version, username, hook)).asPromise map {
      case Connected(channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          (event \ "t").as[String] match {
            case "talk" ⇒ hub ! Talk(
              (event \ "d" \ "txt").as[String],
              (event \ "d" \ "u").as[String]
            )
          }
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, channel)
    }

  def addEntry(entry: model.Entry): IO[Unit] = io {
    hub ! Entry(entry)
  }

  def removeHook(hook: model.Hook): IO[Unit] = io {
    hub ! RemoveHook(hook)
  }

  def addHook(hook: model.Hook): IO[Unit] = io {
    hub ! AddHook(hook)
  }

  def biteHook(hook: model.Hook, game: model.DbGame): IO[Unit] = io {
    hub ! BiteHook(hook, game)
  }
}
