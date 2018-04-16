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

  import lila.common.HttpStream._

  def apply(init: Game.WithInitialFen): Enumerator[String] = {

    val id = init.game.id

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[JsObject](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          jsonView gameFull init foreach { json =>
            // prepend the full game JSON at the start of the stream
            channel push json
            // close stream if game is over
            if (init.game.finished) channel.eofAndEnd()
          }

          def receive = {
            case g: Game if g.id == id =>
              pushState(g)
            case FinishGame(g, _, _) if g.id == id =>
              pushState(g) >>- channel.eofAndEnd()
          }

          def pushState(g: Game) = jsonView gameState Game.WithInitialFen(g, init.fen) map channel.push
        }))
        system.lilaBus.subscribe(actor, Symbol(s"moveGame:$id"), 'finishGame)
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    enumerator &> stringify
  }
}
