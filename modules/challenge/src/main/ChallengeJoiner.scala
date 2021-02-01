package lila.challenge

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.{ Color, Mode, Situation }
import scala.util.chaining._

import lila.game.{ Game, Player, Pov, Source }
import lila.user.User

final private class ChallengeJoiner(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(c: Challenge, destUser: Option[User], color: Option[Color]): Fu[Option[Pov]] =
    gameRepo exists c.id flatMap {
      case true                                                           => fuccess(None)
      case _ if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) => fuccess(None)
      case _ =>
        c.challengerUserId.??(userRepo.byId) flatMap { origUser =>
          val game = ChallengeJoiner.createGame(c, origUser, destUser, color)
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
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
    def makeChess(variant: chess.variant.Variant): chess.Game =
      chess.Game(situation = Situation(variant), clock = c.clock.map(_.config.toClock))

    val baseState = c.initialFen.ifTrue(c.variant.fromPosition || c.variant.chess960) flatMap {
      Forsyth.<<<@(c.variant, _)
    }
    val (chessGame, state) = baseState.fold(makeChess(c.variant) -> none[SituationPlus]) {
      case sp @ SituationPlus(sit, _) =>
        val game = chess.Game(
          situation = sit,
          turns = sp.turns,
          startedAtTurn = sp.turns,
          clock = c.clock.map(_.config.toClock)
        )
        if (c.variant.fromPosition && Forsyth.>>(game).initial)
          makeChess(chess.variant.Standard) -> none
        else game                           -> baseState
    }
    val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
    Game
      .make(
        chess = chessGame,
        whitePlayer = Player.make(chess.White, c.finalColor.fold(origUser, destUser), perfPicker),
        blackPlayer = Player.make(chess.Black, c.finalColor.fold(destUser, origUser), perfPicker),
        mode = if (chessGame.board.variant.fromPosition) Mode.Casual else c.mode,
        source = Source.Friend,
        daysPerTurn = c.daysPerTurn,
        pgnImport = None
      )
      .withId(c.id)
      .pipe { g =>
        state.fold(g) { case sit @ SituationPlus(Situation(board, _), _) =>
          g.copy(
            chess = g.chess.copy(
              situation = g.situation.copy(
                board = g.board.copy(history = board.history)
              ),
              turns = sit.turns
            )
          )
        }
      }
      .start
  }
}
