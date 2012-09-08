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
import scalaz.{ Success, Failure }

import game.{ Pov, PovRef }
import user.User
import chess.Color
import socket.{ PingVersion, Quit, Resync }
import socket.Util.connectionFail
import security.Flood
import implicits.RichJs._

final class Socket(
    getWatcherPov: (String, String) ⇒ IO[Option[Pov]],
    getPlayerPov: String ⇒ IO[Option[Pov]],
    hand: Hand,
    hubMaster: ActorRef,
    messenger: Messenger,
    moveNotifier: MoveNotifier,
    flood: Flood) {

  private val timeoutDuration = 1 second
  implicit private val timeout = Timeout(timeoutDuration)

  def blockingVersion(gameId: String): Int = Await.result(
    hubMaster ? GetGameVersion(gameId) mapTo manifest[Int],
    timeoutDuration)

  def send(progress: Progress) {
    send(progress.game.id, progress.events)
  }

  def send(gameId: String, events: List[Event]) {
    hubMaster ! GameEvents(gameId, events)
  }

  private def controller(
    hub: ActorRef,
    uid: String,
    member: Member,
    povRef: PovRef): JsValue ⇒ Unit =
    if (member.owner) (e: JsValue) ⇒ e str "t" match {
      case Some("p") ⇒ e int "v" foreach { v ⇒
        hub ! PingVersion(uid, v)
      }
      case Some("talk") ⇒ for {
        txt ← e str "d"
        if member.canChat
        if flood.allowMessage(uid, txt)
      } {
        val events = messenger.playerMessage(povRef, txt).unsafePerformIO
        hub ! Events(events)
      }
      case Some("move") ⇒ parseMove(e) foreach {
        case (orig, dest, prom, blur, lag) ⇒ {
          hub ! Ack(uid)
          hand.play(povRef, orig, dest, prom, blur, lag) onSuccess {
            case Failure(fs) ⇒ {
              hub ! Resync(uid)
              println(fs.shows)
            }
            case Success((events, fen, lastMove)) ⇒ {
              send(povRef.gameId, events)
              moveNotifier(povRef.gameId, fen, lastMove)
            }
          }
        }
      }
      case Some("moretime") ⇒ (for {
        res ← hand moretime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op).unsafePerformIO
      case Some("outoftime") ⇒ (for {
        res ← hand outoftime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op).unsafePerformIO
      case _ ⇒
    }

    else (e: JsValue) ⇒ e str "t" match {
      case Some("p") ⇒ e int "v" foreach { v ⇒
        hub ! PingVersion(uid, v)
      }
      case Some("talk") ⇒ for {
        txt ← e str "d"
        if member.canChat
        if flood.allowMessage(uid, txt)
      } {
        val events = messenger.watcherMessage(
          povRef.gameId,
          member.username,
          txt).unsafePerformIO
        hub ! Events(events)
      }
      case _ ⇒
    }

  def joinWatcher(
    gameId: String,
    colorName: String,
    version: Option[Int],
    uid: Option[String],
    user: Option[User]): IO[SocketPromise] =
    getWatcherPov(gameId, colorName) map { join(_, false, version, uid, user) }

  def joinPlayer(
    fullId: String,
    version: Option[Int],
    uid: Option[String],
    user: Option[User]): IO[SocketPromise] =
    getPlayerPov(fullId) map { join(_, true, version, uid, user) }

  private def parseMove(event: JsValue) = for {
    d ← event obj "d"
    orig ← d str "from"
    dest ← d str "to"
    prom = d str "promotion"
    blur = (d int "b") == Some(1)
    lag = d int "lag"
  } yield (orig, dest, prom, blur, lag | 0)

  private def join(
    povOption: Option[Pov],
    owner: Boolean,
    versionOption: Option[Int],
    uidOption: Option[String],
    user: Option[User]): SocketPromise =
    ((povOption |@| uidOption |@| versionOption) apply {
      (pov: Pov, uid: String, version: Int) ⇒
        (for {
          hub ← hubMaster ? GetHub(pov.gameId) mapTo manifest[ActorRef]
          socket ← hub ? Join(
            uid = uid,
            user = user,
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
