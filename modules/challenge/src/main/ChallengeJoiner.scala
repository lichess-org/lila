package lila.challenge

import chess.format.Fen
import chess.variant.Variant
import chess.{ Mode, Situation, ByColor }
import scala.util.chaining.*

import lila.game.{ Game, Player, Pov, Source }
import lila.user.GameUser

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userApi: lila.user.UserApi,
    onStart: lila.round.OnStart
)(using Executor):

  def apply(c: Challenge, destUser: GameUser): Fu[Either[String, Pov]] =
    gameRepo exists c.id.into(GameId) flatMap {
      if _ then fuccess(Left("The challenge has already been accepted"))
      else
        c.challengerUserId.so(userApi.withPerf(_, c.perfType)) flatMap { origUser =>
          val game = ChallengeJoiner.createGame(c, origUser, destUser)
          (gameRepo insertDenormalized game) andDo onStart(game.id) inject
            Right(Pov(game, !c.finalColor))
        }
    }

private object ChallengeJoiner:

  def createGame(
      c: Challenge,
      origUser: GameUser,
      destUser: GameUser
  ): Game =
    val (chessGame, state) = gameSetup(c.variant, c.timeControl, c.initialFen)
    Game
      .make(
        chess = chessGame,
        players = ByColor: color =>
          Player.make(color, if c.finalColor == color then origUser else destUser),
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
