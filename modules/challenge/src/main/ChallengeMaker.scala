package lila.challenge

import Challenge.TimeControl
import lila.game.{ Game, GameRepo, Pov, Rematches }
import lila.user.User
import lila.game.Player

final class ChallengeMaker(
    userRepo: lila.user.UserRepo,
    gameRepo: GameRepo,
    rematches: Rematches
)(using Executor):

  def makeRematchFor(gameId: GameId, dest: User): Fu[Option[Challenge]] =
    collectDataFor(gameId, dest) flatMapz { (game, challenger, challengerUser) =>
      makeRematch(Pov(game, challenger), challengerUser, dest) dmap some
    }

  def showCanceledRematchFor(gameId: GameId, dest: User, nextId: GameId): Fu[Option[Challenge]] =
    collectDataFor(gameId, dest) flatMapz { (game, challenger, challengerUser) =>
      toChallenge(Pov(game, challenger), challengerUser, dest, nextId) dmap some
    }

  private def collectDataFor(gameId: GameId, dest: User): Future[Option[(Game, Player, Option[User])]] =
    gameRepo.game(gameId) flatMapz { game =>
      game.opponentByUserId(dest.id) ?? { challenger =>
        (challenger.userId ?? userRepo.byId) map {
          (game, challenger, _).some
        }
      }
    }

  private[challenge] def makeRematchOf(game: Game, challenger: User): Fu[Option[Challenge]] =
    Pov.ofUserId(game, challenger.id) ?? { pov =>
      pov.opponent.userId ?? userRepo.byId flatMapz { dest =>
        makeRematch(pov, challenger.some, dest) dmap some
      }
    }

  // pov of the challenger
  private def makeRematch(pov: Pov, challenger: Option[User], dest: User): Fu[Challenge] = for
    nextGameId <- rematches.offer(pov.ref)
    challenge  <- toChallenge(pov, challenger, dest, nextGameId)
  yield challenge

  private def toChallenge(
      pov: Pov,
      challenger: Option[User],
      dest: User,
      nextId: GameId
  ): Fu[Challenge] =
    gameRepo initialFen pov.game map { initialFen =>
      val timeControl = (pov.game.clock, pov.game.daysPerTurn) match
        case (Some(clock), _) => TimeControl.Clock(clock.config)
        case (_, Some(days))  => TimeControl.Correspondence(days)
        case _                => TimeControl.Unlimited
      Challenge.make(
        variant = pov.game.variant,
        initialFen = initialFen,
        timeControl = timeControl,
        mode = pov.game.mode,
        color = (!pov.color).name,
        // for anon, we don't know the secret, but this challenge is only serialized to json and sent to a listening bot anyway,
        // which doesn't use the secret, so we just use an empty string
        challenger = challenger
          .fold(Challenge.Challenger.Anonymous(""))(Challenge.toRegistered(pov.game.variant, timeControl)),
        destUser = dest.some,
        rematchOf = pov.gameId.some,
        id = nextId.some
      )
    }
