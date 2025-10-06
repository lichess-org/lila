package lila.challenge

import chess.format.Fen
import chess.variant.Variant
import chess.{ Position, ByColor, Rated }

import lila.core.user.GameUser

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart
)(using Executor, Scheduler):

  def apply(c: Challenge, destUser: GameUser): FuRaise[String, Pov] = for
    exists <- gameRepo.exists(c.gameId)
    _ <- raiseIf(exists)("The challenge has already been accepted")
    origUser <- c.challengerUserId.so(userApi.byIdWithPerf(_, c.perfType))
    game = ChallengeJoiner.createGame(c, origUser, destUser)
    _ <- gameRepo.insertDenormalized(game)
    _ <- onStartOrRetry(game.id).recover: _ =>
      logger.error(s"onStart failed for game ${game.id}")
  yield Pov(game, !c.finalColor)

  private def onStartOrRetry(id: GameId, retries: Int = 3): Funit =
    onStart
      .exec(id)
      .recoverWith:
        case _ if retries > 0 =>
          logger.warn(s"onStart failed for game $id. Retries left: $retries")
          lila.common.LilaFuture.delay(500.millis)(onStartOrRetry(id, retries - 1))
      .void

private object ChallengeJoiner:

  def createGame(
      c: Challenge,
      origUser: GameUser,
      destUser: GameUser
  ): Game =
    val (chessGame, state) = gameSetup(c.variant, c.timeControl, c.initialFen)
    lila.core.game
      .newGame(
        chess = chessGame,
        players = ByColor: color =>
          lila.game.Player.make(color, if c.finalColor == color then origUser else destUser),
        rated = c.rated.map(_ && !chessGame.position.variant.fromPosition),
        source = lila.core.game.Source.Friend,
        daysPerTurn = c.daysPerTurn,
        pgnImport = None,
        rules = c.rules
      )
      .withId(c.gameId)
      .pipe(addGameHistory(state))
      .start

  def gameSetup(
      variant: Variant,
      tc: Challenge.TimeControl,
      initialFen: Option[Fen.Full]
  ): (chess.Game, Option[Position.AndFullMoveNumber]) =

    def makeChess(variant: Variant): chess.Game =
      chess.Game(position = variant.initialPosition, clock = tc.realTime.map(_.toClock))

    val baseState = initialFen
      .ifTrue(variant.fromPosition || variant.chess960)
      .flatMap:
        Fen.readWithMoveNumber(variant, _)

    baseState.fold(makeChess(variant) -> none[Position.AndFullMoveNumber]): sp =>
      val game = chess.Game(
        position = sp.position,
        ply = sp.ply,
        startedAtPly = sp.ply,
        clock = tc.realTime.map(_.toClock)
      )
      if variant.fromPosition && Fen.write(game).isInitial then makeChess(chess.variant.Standard) -> none
      else game -> baseState

  def addGameHistory(position: Option[Position.AndFullMoveNumber])(game: Game): Game =
    position.fold(game): sp =>
      game.copy(
        chess = game.chess.copy(
          position = game.position.copy(history = sp.position.history),
          ply = sp.ply
        )
      )
