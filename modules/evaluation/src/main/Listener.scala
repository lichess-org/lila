package lila.evaluation

import akka.actor._
import chess.{ White, Black }
import lila.user.User
import lila.hub.actorApi.evaluation._

private[evaluation] final class Listener(evaluator: Evaluator) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case lila.game.actorApi.FinishGame(game, white, black) => if (game.rated) {
      white foreach { evaluator.autoGenerate(_, false, false) }
      black foreach { evaluator.autoGenerate(_, false, false) }
    }

    case user: User => evaluator.generate(user, true)

    case AutoCheck(userId) => evaluator.autoGenerate(userId, true, false)

    case Refresh(userId) => evaluator.autoGenerate(userId, false, true)
  }
}
