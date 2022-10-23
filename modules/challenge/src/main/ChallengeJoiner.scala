package lila.challenge

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import chess.format.FEN
import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.variant.Variant
import chess.{ Color, Mode, Situation }
import scala.util.chaining._

import lila.game.{ Game, Player, Pov, Source }
import lila.user.User

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(c: Challenge, destUser: Option[User], color: Option[Color]): Fu[Validated[String, Pov]] =
    gameRepo exists c.id flatMap {
      case true => fuccess(Invalid("The challenge has already been accepted"))
      case _ if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) =>
        fuccess(Invalid("This color has already been chosen"))
      case _ =>
        c.challengerUserId.??(userRepo.byId) flatMap { origUser =>
          val game = ChallengeJoiner.createGame(c, origUser, destUser, color)
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject
            Valid(Pov(game, !c.finalColor))
        }
    }
}

private object ChallengeJoiner {

  def createGame(
      c: Challenge,
      origUser: Option[User],
      destUser: Option[User],
      color: Option[Color]
  ): Game = {
    val (chessGame, state) = gameSetup(c.variant, c.timeControl, c.initialFen)
    Game
      .make(
        chess = chessGame,
        whitePlayer = Player.make(chess.White, c.finalColor.fold(origUser, destUser), _(c.perfType)),
        blackPlayer = Player.make(chess.Black, c.finalColor.fold(destUser, origUser), _(c.perfType)),
        mode = if (chessGame.board.variant.fromPosition) Mode.Casual else c.mode,
        source = Source.Friend,
        daysPerTurn = c.daysPerTurn,
        pgnImport = None,
        rules = c.rules
      )
      .withId(c.id)
      .pipe(addGameHistory(state))
      .start
  }

  def gameSetup(
      variant: Variant,
      tc: Challenge.TimeControl,
      initialFen: Option[FEN]
  ): (chess.Game, Option[SituationPlus]) = {

    def makeChess(variant: Variant): chess.Game =
      chess.Game(situation = Situation(variant), clock = tc.realTime.map(_.toClock))

    val baseState = initialFen.ifTrue(variant.fromPosition || variant.chess960) flatMap {
      Forsyth.<<<@(variant, _)
    }

    baseState.fold(makeChess(variant) -> none[SituationPlus]) { case sp @ SituationPlus(sit, _) =>
      val game = chess.Game(
        situation = sit,
        turns = sp.turns,
        startedAtTurn = sp.turns,
        clock = tc.realTime.map(_.toClock)
      )
      if (variant.fromPosition && Forsyth.>>(game).initial)
        makeChess(chess.variant.Standard) -> none
      else game                           -> baseState
    }
  }

  def addGameHistory(position: Option[SituationPlus])(game: Game): Game =
    position.fold(game) { case sit @ SituationPlus(Situation(board, _), _) =>
      game.copy(
        chess = game.chess.copy(
          situation = game.situation.copy(
            board = game.board.copy(history = board.history)
          ),
          turns = sit.turns
        )
      )
    }
}
