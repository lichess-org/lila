package lila.challenge

import chess.format.Fen
import chess.variant.Variant
import chess.{ ByColor, Mode, Situation }

import lila.core.user.GameUser

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart
)(using Executor):

  def apply(c: Challenge, destUser: GameUser): Fu[Either[String, Pov]] =
    gameRepo
      .exists(c.gameId)
      .flatMap:
        if _ then fuccess(Left("The challenge has already been accepted"))
        else
          c.challengerUserId
            .so(userApi.byIdWithPerf(_, c.perfType))
            .flatMap: origUser =>
              val game = ChallengeJoiner.createGame(c, origUser, destUser)
              gameRepo
                .insertDenormalized(game)
                .inject:
                  onStart.exec(game.id)
                  Right(Pov(game, !c.finalColor))

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
        mode = if chessGame.board.variant.fromPosition then Mode.Casual else c.mode,
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
  ): (chess.Game, Option[Situation.AndFullMoveNumber]) =

    def makeChess(variant: Variant): chess.Game =
      chess.Game(situation = Situation(variant), clock = tc.realTime.map(_.toClock))

    val baseState = initialFen
      .ifTrue(variant.fromPosition || variant.chess960)
      .flatMap:
        Fen.readWithMoveNumber(variant, _)

    baseState.fold(makeChess(variant) -> none[Situation.AndFullMoveNumber]): sp =>
      val game = chess.Game(
        situation = sp.situation,
        ply = sp.ply,
        startedAtPly = sp.ply,
        clock = tc.realTime.map(_.toClock)
      )
      if variant.fromPosition && Fen.write(game).isInitial then makeChess(chess.variant.Standard) -> none
      else game                                                                                   -> baseState

  def addGameHistory(position: Option[Situation.AndFullMoveNumber])(game: Game): Game =
    position.fold(game): sp =>
      game.copy(
        chess = game.chess.copy(
          situation = game.situation.copy(
            board = game.board.copy(history = sp.situation.board.history)
          ),
          ply = sp.ply
        )
      )
