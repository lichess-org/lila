package lila.challenge

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import chess.format.Fen
import chess.variant.Variant
import chess.{ Mode, Situation }
import scala.util.chaining.*

import lila.game.{ Game, Player, Pov, Source }
import lila.user.User

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userApi: lila.user.UserApi,
    onStart: lila.round.OnStart
)(using Executor):

  def apply(c: Challenge, destUser: Option[User.WithPerfs]): Fu[Validated[String, Pov]] =
    gameRepo exists c.id.into(GameId) flatMap {
      if _ then fuccess(Invalid("The challenge has already been accepted"))
      else
        c.challengerUserId.so(userApi.withPerfs) flatMap { origUser =>
          val game = ChallengeJoiner.createGame(c, origUser, destUser)
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject
            Valid(Pov(game, !c.finalColor))
        }
    }

private object ChallengeJoiner:

  def createGame(
      c: Challenge,
      origUser: Option[User.WithPerfs],
      destUser: Option[User.WithPerfs]
  ): Game =
    val (chessGame, state) = gameSetup(c.variant, c.timeControl, c.initialFen)
    Game
      .make(
        chess = chessGame,
        whitePlayer = Player.make(chess.White, c.finalColor.fold(origUser, destUser).map(_ only c.perfType)),
        blackPlayer = Player.make(chess.Black, c.finalColor.fold(destUser, origUser).map(_ only c.perfType)),
        mode = if chessGame.board.variant.fromPosition then Mode.Casual else c.mode,
        source = Source.Friend,
        daysPerTurn = c.daysPerTurn,
        pgnImport = None,
        rules = c.rules
      )
      .withId(c.id into GameId)
      .pipe(addGameHistory(state))
      .start

  def gameSetup(
      variant: Variant,
      tc: Challenge.TimeControl,
      initialFen: Option[Fen.Epd]
  ): (chess.Game, Option[Situation.AndFullMoveNumber]) =

    def makeChess(variant: Variant): chess.Game =
      chess.Game(situation = Situation(variant), clock = tc.realTime.map(_.toClock))

    val baseState = initialFen.ifTrue(variant.fromPosition || variant.chess960) flatMap {
      Fen.readWithMoveNumber(variant, _)
    }

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
