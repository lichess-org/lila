package lila.evaluation

import akka.actor._
import chess.{ White, Black }
import lila.hub.actorApi.evaluation._
import lila.user.User

private[evaluation] final class Listener(evaluator: Evaluator) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case lila.game.actorApi.FinishGame(game, white, black) => if (game.rated) {
      List(
        game.whitePlayer -> white,
        game.blackPlayer -> black
      ) foreach {
          case (p, Some(u)) => evaluator.autoGenerate(u, false, false, suspiciousHold = p.hasSuspiciousHoldAlert)
          case _            =>
        }
    }

    case user: User        => evaluator.generate(user, true)

    case AutoCheck(userId) => evaluator.autoGenerate(userId, true, false)

    case Refresh(userId)   => evaluator.autoGenerate(userId, false, true)
  }
}
