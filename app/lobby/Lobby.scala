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

final class Lobby(hub: ActorRef, hookPool: ActorRef) {

  type PromiseType = Promise[(Iteratee[JsValue, _], Channel)]

  implicit val timeout = Timeout(1 second)

  def join(uid: String, version: Int, hook: Option[String]): PromiseType =
    (hub ? Join(uid, version, hook)).asPromise map {
      case Connected(channel) ⇒
        hook foreach { h ⇒ hookPool ! HookPool.Register(h) }
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          (event \ "t").as[String] match {
            case "talk" ⇒ hub ! Talk(
              (event \ "data" \ "txt").as[String],
              (event \ "data" \ "u").as[String]
            )
          }
        } mapDone { _ ⇒
          hook foreach { h ⇒ hookPool ! HookPool.Unregister(h) }
          hub ! Quit(uid)
        }
        (iteratee, channel)
    }

  def addEntry(entry: model.Entry): IO[Unit] = io {
    hub ! Entry(entry)
  }

  def removeHook(hook: model.Hook): IO[Unit] = io {
    hookPool ! HookPool.Unregister(hook.ownerId)
    hub ! RemoveHook(hook)
  }

  def addHook(hook: model.Hook): IO[Unit] = io {
    hub ! AddHook(hook)
  }

  def biteHook(hook: model.Hook, game: model.DbGame): IO[Unit] = io {
    hub ! BiteHook(hook, game)
  }
}
