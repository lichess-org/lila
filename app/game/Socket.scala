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
import model.{ DbGame, Pov }

final class Socket(
    gameRepo: GameRepo,
    hubMemo: HubMemo,
    messenger: Messenger) {

  implicit val timeout = Timeout(1 second)

  def listener(
    hub: ActorRef,
    member: Member,
    gameId: String): JsValue ⇒ Unit = member match {
    case Watcher(_, _, _) ⇒ (_: JsValue) ⇒ Unit
    case Owner(_, _, _) ⇒ (e: JsValue) ⇒ (e \ "t").as[String] match {
      case "talk" ⇒ (e \ "d").as[String] |> { txt ⇒
        hub ! Events(
          messenger.playerMessage(gameId, member.color, txt).unsafePerformIO
        )
      }
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
              listener(hub, member, gameId)
            ) mapDone { _ ⇒
                hub ! Quit(uid)
              },
              member.channel)
        }
      promise | Promise.pure {
        Done[JsValue, Unit]((), Input.EOF) -> (Enumerator[JsValue](
          JsObject(Seq("error" -> JsString("Invalid request")))
        ) andThen Enumerator.enumInput(Input.EOF))
      }
    }
}
