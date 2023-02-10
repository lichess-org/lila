package lila.challenge

import Challenge.TimeControl
import com.github.blemale.scaffeine.Cache

import lila.game.{ Game, GameRepo, Pov, Rematches }
import lila.memo.CacheApi
import lila.user.User

final class ChallengeMaker(
    userRepo: lila.user.UserRepo,
    gameRepo: GameRepo,
    rematches: Rematches
)(using Executor):

  def makeRematchFor(gameId: GameId, dest: User): Fu[Option[Challenge]] =
    gameRepo.game(gameId) flatMapz { game =>
      game.opponentByUserId(dest.id) ?? { challenger =>
        (challenger.userId ?? userRepo.byId) flatMap {
          makeRematch(Pov(game, challenger), _, dest) dmap some
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
  private def makeRematch(pov: Pov, challenger: Option[User], dest: User): Fu[Challenge] = for {
    initialFen <- gameRepo initialFen pov.game
    nextGameId <- rematches.offer(pov.ref)
  } yield
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
      id = nextGameId.some
    )
