package lila
package round

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Await

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

import scalaz.effects._

import game.{ Pov, PovRef }
import chess.Color
import socket.{ PingVersion, Quit }
import socket.Util.connectionFail
import implicits.RichJs._

final class Socket(
    getWatcherPov: (String, String) ⇒ IO[Option[Pov]],
    getPlayerPov: String ⇒ IO[Option[Pov]],
    hand: Hand,
    hubMaster: ActorRef,
    messenger: Messenger) {

  private val timeoutDuration = 1 second
  implicit private val timeout = Timeout(timeoutDuration)

  def blockingVersion(gameId: String): Int = Await.result(
    hubMaster ? GetGameVersion(gameId) mapTo manifest[Int],
    timeoutDuration)

  def send(progress: Progress): IO[Unit] =
    send(progress.game.id, progress.events)

  def send(gameId: String, events: List[Event]): IO[Unit] = io {
    hubMaster ! GameEvents(gameId, events)
  }

  private def controller(
    hub: ActorRef,
    uid: String,
    member: Member,
    povRef: PovRef): JsValue ⇒ Unit =
    if (member.owner) (e: JsValue) ⇒ e str "t" match {
      case Some("talk") ⇒ e str "d" foreach { txt ⇒
        val events = messenger.playerMessage(povRef, txt).unsafePerformIO
        hub ! Events(events)
      }
      case Some("move") ⇒ parseMove(e) foreach {
        case (orig, dest, prom, blur) ⇒
          hand.play(povRef, orig, dest, prom, blur) flatMap { events ⇒
            events.fold(putFailures, send(povRef.gameId, _))
          } unsafePerformIO
      }
      case Some("moretime") ⇒ (for {
        res ← hand moretime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op).unsafePerformIO
      case Some("outoftime") ⇒ (for {
        res ← hand outoftime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op).unsafePerformIO
      case Some("p") ⇒ e int "v" foreach { v ⇒
        hub ! PingVersion(uid, v)
      }
      case _ ⇒
    }

    else (e: JsValue) ⇒ e str "t" match {
      case Some("p") ⇒ e int "v" foreach { v ⇒
        hub ! PingVersion(uid, v)
      }
      case _ ⇒
    }

  def joinWatcher(
    gameId: String,
    colorName: String,
    version: Option[Int],
    uid: Option[String],
    username: Option[String]): IO[SocketPromise] =
    getWatcherPov(gameId, colorName) map { join(_, false, version, uid, username) }

  def joinPlayer(
    fullId: String,
    version: Option[Int],
    uid: Option[String],
    username: Option[String]): IO[SocketPromise] =
    getPlayerPov(fullId) map { join(_, true, version, uid, username) }

  private def parseMove(event: JsValue) = for {
    d ← event obj "d"
    orig ← d str "from"
    dest ← d str "to"
    prom = d str "promotion"
    blur = (d int "b") == Some(1)
  } yield (orig, dest, prom, blur)

  private def join(
    povOption: Option[Pov],
    owner: Boolean,
    versionOption: Option[Int],
    uidOption: Option[String],
    username: Option[String]): SocketPromise =
    ((povOption |@| uidOption |@| versionOption) apply {
      (pov: Pov, uid: String, version: Int) ⇒
        (for {
          hub ← hubMaster ? GetHub(pov.gameId) mapTo manifest[ActorRef]
          socket ← hub ? Join(
            uid = uid,
            username = username,
            version = version,
            color = pov.color,
            owner = owner
          ) map {
              case Connected(enumerator, member) ⇒ (
                Iteratee.foreach[JsValue](
                  controller(hub, uid, member, PovRef(pov.gameId, member.color))
                ) mapDone { _ ⇒
                    hub ! Quit(uid)
                  },
                  enumerator)
            }
        } yield socket).asPromise: SocketPromise
    }) | connectionFail
}
