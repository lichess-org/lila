package lila.challenge

import Challenge.TimeControl
import lila.game.{ Game, Pov }
import lila.user.User

final class ChallengeMaker(
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def makeRematchFor(gameId: Game.ID, dest: User): Fu[Option[Challenge]] =
    gameRepo game gameId flatMap {
      _ ?? { game =>
        game.opponentByUserId(dest.id).flatMap(_.userId) ?? userRepo.byId flatMap {
          _ ?? { challenger =>
            Pov(game, challenger) ?? { pov =>
              fuccess(makeRematch(pov, challenger, dest)) dmap some
            }
          }
        }
      }
    }

  def makeRematchOf(game: Game, challenger: User): Fu[Option[Challenge]] =
    Pov.ofUserId(game, challenger.id) ?? { pov =>
      pov.opponent.userId ?? userRepo.byId flatMap {
        _ ?? { dest =>
          fuccess(makeRematch(pov, challenger, dest)) dmap some
        }
      }
    }

  // pov of the challenger
  private def makeRematch(pov: Pov, challenger: User, dest: User): Challenge = {
    val timeControl = (pov.game.clock, pov.game.daysPerTurn) match {
      case (Some(clock), _) => TimeControl.Clock(clock.config)
      case (_, Some(days))  => TimeControl.Correspondence(days)
      case _                => TimeControl.Unlimited
    }
    Challenge.make(
      variant = pov.game.variant,
      initialSfen = pov.game.initialSfen,
      timeControl = timeControl,
      mode = pov.game.mode,
      color = (!pov.color).name,
      challenger = Challenge.toRegistered(pov.game.variant, timeControl)(challenger),
      destUser = dest.some,
      rematchOf = pov.gameId.some
    )
  }
}
