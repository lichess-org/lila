package lila.evaluation

import akka.actor._
import chess.{ Speed, White, Black }
import lila.hub.actorApi.evaluation._
import lila.user.User

private[evaluation] final class Listener(evaluator: Evaluator) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case lila.game.actorApi.FinishGame(game, white, black) =>
      PerfType(PerfPicker.noPoolKey(game.speed, game.variant)) ifTrue game.rated map { perfType =>
        List(
          game.whitePlayer -> white,
          game.blackPlayer -> black
        ) foreach {
            case (p, Some(u)) => evaluator.autoGenerate(
              user = u,
              perfType = perfType,
              important = p.wins && game.isTournament && game.speed != Speed.Bullet,
              forceRefresh = false,
              suspiciousHold = p.hasSuspiciousHoldAlert)
            case _ =>
          }
      }

    case user: User        => evaluator.generate(user, true)

    case AutoCheck(userId) => evaluator.autoGenerate(userId, true, false)

    case Refresh(userId)   => evaluator.autoGenerate(userId, false, true)
  }
}
