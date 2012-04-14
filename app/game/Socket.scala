package lila
package game

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scalaz.effects._

import chess.Color
import db.GameRepo
import model.{ DbGame, Pov, PovRef, Progress, Event }

final class Socket(
    gameRepo: GameRepo,
    hand: Hand,
    hubMemo: HubMemo,
    messenger: Messenger) {

  implicit val timeout = Timeout(1 second)

  implicit def richJsObject(js: JsObject) = new {
    def str(key: String): Option[String] = js.value get key map (_.as[String])
    def obj(key: String): Option[JsObject] = js.value get key map (_.as[JsObject])
  }

  def send(progress: Progress): IO[Unit] =
    send(progress.game.id, progress.events)

  def send(gameId: String, events: List[Event]): IO[Unit] = io {
    (hubMemo get gameId) ! Events(events)
  }

  def listener(
    hub: ActorRef,
    member: Member,
    povRef: PovRef): JsValue ⇒ Unit = member match {
    case Watcher(_, _, _) ⇒ (_: JsValue) ⇒ Unit
    case Owner(_, color, _) ⇒ (e: JsValue) ⇒ (e \ "t").as[String] match {
      case "talk" ⇒ (e \ "d").as[String] |> { txt ⇒
        hub ! Events(
          messenger.playerMessage(povRef, txt).unsafePerformIO
        )
      }
      case "move" ⇒ for {
        d ← e.as[JsObject] obj "d"
        orig ← d str "from"
        dest ← d str "to"
        promotion = d str "promotion"
        op = for {
          events ← hand.play(povRef, orig, dest, promotion)
          _ ← events.fold(
            errors ⇒ putStrLn(errors.list mkString "\n"),
            events ⇒ send(povRef.gameId, events))
        } yield ()
      } op.unsafePerformIO
    }
  }

  def join(
    gameId: String,
    colorName: String,
    uid: String,
    version: Int,
    playerId: Option[String],
    username: Option[String]): IO[SocketPromise] =
    gameRepo gameOption gameId map { gameOption ⇒
      val promise: Option[SocketPromise] = for {
        game ← gameOption
        color ← Color(colorName)
        hub = hubMemo get gameId
      } yield (hub ? Join(
        uid = uid,
        version = version,
        color = color,
        owner = (playerId flatMap game.player).isDefined,
        username = username
      )).asPromise map {
          case Connected(member) ⇒ (
            Iteratee.foreach[JsValue](
              listener(hub, member, PovRef(gameId, member.color))
            ) mapDone { _ ⇒
                hub ! Quit(uid)
              },
              member.channel)
        }
      promise | connectionFail
    }

  private def connectionFail = Promise.pure {
    Done[JsValue, Unit]((), Input.EOF) -> (Enumerator[JsValue](
      JsObject(Seq("error" -> JsString("Invalid request")))
    ) andThen Enumerator.enumInput(Input.EOF))
  }

}
