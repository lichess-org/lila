package lila.bot

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import chess.format.FEN

import lila.game.actorApi.FinishGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.round.MoveEvent
import lila.user.User

final class GameStateStream(
    system: ActorSystem,
    jsonView: BotJsonView
) {

  def apply(id: Game.ID, current: Game.ID => Fu[Option[Game]]): Enumerator[String] = {

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[JsObject](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          withFen foreach { wf =>
            jsonView gameFull wf foreach { json =>
              // prepend the full game JSON at the start of the stream
              channel push json
              // close stream if game is over
              if (wf.game.finished) channel.eofAndEnd()
            }
          }

          def receive = {
            case MoveEvent(gameId, _, _) if gameId == id => pushState
            case FinishGame(game, _, _) if game.id == id =>
              pushState
              channel.eofAndEnd()
          }

          def pushState = withFen flatMap jsonView.gameState foreach channel.push

          def withFen = current(id) flatten "No game found" flatMap GameRepo.withInitialFen
        }))
        system.lilaBus.subscribe(actor, Symbol(s"moveEvent:$id"), 'finishGame)
        stream = actor.some
      },
      onComplete = {
        stream.foreach { actor =>
          system.lilaBus.unsubscribe(actor)
          actor ! PoisonPill
        }
      }
    )

    enumerator &> stringify
  }

  private val stringify =
    Enumeratee.map[JsObject].apply[String] { js =>
      Json.stringify(js) + "\n"
    }
}
