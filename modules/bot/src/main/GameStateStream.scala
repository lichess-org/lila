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
          pushFull // prepend the full game JSON at the start of the stream
          def receive = {
            case MoveEvent(gameId, _, _) if gameId == id => pushState
            case FinishGame(game, _, _) if game.id == id => pushState
          }
          def pushState = for {
            wf <- withFen
            json <- jsonView.gameState(wf.game, wf.fen)
          } channel push json
          def pushFull = for {
            wf <- withFen
            json <- jsonView.gameFull(wf.game, wf.fen)
          } channel push json
          def withFen = for {
            gameOption <- current(id)
            game <- gameOption.fold[Fu[Game]](fufail("No game found"))(fuccess)
            withFen <- GameRepo withInitialFen game
          } yield withFen
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

  private val withInitialFen =
    Enumeratee.mapM[Game].apply[Game.WithInitialFen](GameRepo.withInitialFen)

  private val toJson =
    Enumeratee.mapM[Game.WithInitialFen].apply[JsObject] {
      case Game.WithInitialFen(game, initialFen) => jsonView.gameState(game, initialFen)
    }

  private val stringify =
    Enumeratee.map[JsObject].apply[String] { js =>
      Json.stringify(js) + "\n"
    }
}
