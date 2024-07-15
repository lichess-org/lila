package lila.challenge

import lila.core.game.Player
import lila.core.user.{ GameUser, WithPerf }
import lila.game.{ GameRepo, Rematches }

import Challenge.TimeControl

final class ChallengeMaker(
    userApi: lila.core.user.UserApi,
    gameRepo: GameRepo,
    rematches: Rematches
)(using Executor):

  def makeRematchFor(gameId: GameId, dest: User): Fu[Option[Challenge]] =
    collectDataFor(gameId, dest).flatMapz: data =>
      makeRematch(Pov(data.game, data.challenger), data.orig, data.dest).dmap(some)

  def showCanceledRematchFor(gameId: GameId, dest: User, nextId: GameId): Fu[Option[Challenge]] =
    collectDataFor(gameId, dest).flatMapz: data =>
      toChallenge(Pov(data.game, data.challenger), data.orig, data.dest, nextId).dmap(some)

  private case class Data(game: Game, challenger: Player, orig: GameUser, dest: WithPerf)

  private def collectDataFor(gameId: GameId, dest: User): Future[Option[Data]] =
    gameRepo
      .game(gameId)
      .flatMapz: game =>
        game
          .opponentOf(dest)
          .so: challenger =>
            for
              orig <- challenger.userId.so(userApi.byIdWithPerf(_, game.perfKey))
              dest <- userApi.withPerf(dest, game.perfKey)
            yield Data(game, challenger, orig, dest).some

  private[challenge] def makeRematchOf(game: Game, challenger: User): Fu[Option[Challenge]] =
    Pov(game, challenger.id).so: pov =>
      pov.opponent.userId.so(userApi.byIdWithPerf(_, game.perfKey)).flatMapz { dest =>
        for
          challenger <- userApi.withPerf(challenger, game.perfKey)
          rematch    <- makeRematch(pov, challenger.some, dest)
        yield rematch.some
      }

  // pov of the challenger
  private def makeRematch(pov: Pov, challenger: GameUser, dest: WithPerf): Fu[Challenge] =
    for
      nextGameId <- rematches.offer(pov.ref)
      challenge  <- toChallenge(pov, challenger, dest, nextGameId)
    yield challenge

  private def toChallenge(
      pov: Pov,
      challenger: GameUser,
      dest: WithPerf,
      nextId: GameId
  ): Fu[Challenge] =
    gameRepo.initialFen(pov.game).map { initialFen =>
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
          .fold(Challenge.Challenger.Anonymous(""))(Challenge.toRegistered),
        destUser = dest.some,
        rematchOf = pov.gameId.some,
        id = nextId.some
      )
    }
