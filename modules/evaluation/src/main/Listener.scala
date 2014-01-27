package lila.evaluation

import akka.actor._
import chess.{ White, Black }

private[evaluation] final class Listener(evaluator: Evaluator) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case lila.game.actorApi.FinishGame(game, white, black) ⇒ if (game.rated) {
      white foreach { user ⇒ evaluator.autoGenerate(user, game player White) }
      black foreach { user ⇒ evaluator.autoGenerate(user, game player Black) }
    }
  }
}
