package lila
package game

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

import scalaz.effects._

import chess.Color
import model.{ DbGame, Pov, PovRef, Progress, Event }
import socket.Util
import RichJs._

final class Socket(
    getGame: String ⇒ IO[Option[DbGame]],
    hand: Hand,
    hubMemo: HubMemo,
    messenger: Messenger) {

  implicit val timeout = Timeout(1 second)

  def send(progress: Progress): IO[Unit] =
    send(progress.game.id, progress.events)

  def send(gameId: String, events: List[Event]): IO[Unit] = io {
    (hubMemo get gameId) ! Events(events)
  }

  def controller(
    hub: ActorRef,
    member: Member,
    povRef: PovRef): JsValue ⇒ Unit = member match {
    case Watcher(_, _) ⇒ (_: JsValue) ⇒ Unit
    case Owner(_, color) ⇒ (e: JsValue) ⇒ (e str "t" match {
      case Some("talk") ⇒ (e str "d" map { txt ⇒
        messenger.playerMessage(povRef, txt) map { hub ! Events(_) }
      }) | io()
      case Some("move") ⇒ (for {
        d ← e.as[JsObject] obj "d"
        orig ← d str "from"
        dest ← d str "to"
        promotion = d str "promotion"
        blur = (d int "b") == Some(1)
        op = for {
          events ← hand.play(povRef, orig, dest, promotion, blur)
          _ ← events.fold(putFailures, events ⇒ send(povRef.gameId, events))
        } yield ()
      } yield op) | io()
      case Some("moretime") ⇒ for {
        res ← hand moretime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op
      case Some("outoftime") ⇒ for {
        res ← hand outoftime povRef
        op ← res.fold(putFailures, events ⇒ io(hub ! Events(events)))
      } yield op
      case _ ⇒ io()
    }).unsafePerformIO
  }

  def join(
    gameId: String,
    colorName: String,
    versionOption: Option[Int],
    playerId: Option[String]): IO[SocketPromise] =
    getGame(gameId) map { gameOption ⇒
      val promise: Option[SocketPromise] = for {
        game ← gameOption
        color ← Color(colorName)
        version ← versionOption
        uid = Util.uid
        hub = hubMemo get gameId
      } yield (hub ? Join(
        uid = uid,
        version = version,
        color = color,
        owner = (playerId flatMap game.player).isDefined
      )).asPromise map {
          case Connected(member) ⇒ (
            Iteratee.foreach[JsValue](
              controller(hub, member, PovRef(gameId, member.color))
            ) mapDone { _ ⇒
                hub ! Quit(uid)
                scheduleForDeletion(hub, gameId)
              },
              member.channel)
        }
      promise | Util.connectionFail
    }

  private def scheduleForDeletion(hub: ActorRef, gameId: String) {
    Akka.system.scheduler.scheduleOnce(10 seconds) {
      hub ! IfEmpty(hubMemo remove gameId)
    }
  }
}
