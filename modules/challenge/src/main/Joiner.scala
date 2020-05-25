package lila.challenge

import scala.util.chaining._

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.{ Color, Mode, Situation }
import lila.game.{ Game, Player, Pov, Source }
import lila.user.User

final private class Joiner(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(c: Challenge, destUser: Option[User], color: Option[Color]): Fu[Option[Pov]] =
    gameRepo exists c.id flatMap {
      case true                                                           => fuccess(None)
      case _ if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) => fuccess(None)
      case _ =>
        c.challengerUserId.??(userRepo.byId) flatMap { challengerUser =>
          def makeChess(variant: chess.variant.Variant): chess.Game =
            chess.Game(situation = Situation(variant), clock = c.clock.map(_.config.toClock))

          val baseState = c.initialFen.ifTrue(c.variant.fromPosition) flatMap { fen =>
            Forsyth.<<<@(chess.variant.FromPosition, fen.value)
          }
          val (chessGame, state) = baseState.fold(makeChess(c.variant) -> none[SituationPlus]) {
            case sit @ SituationPlus(s, _) =>
              val game = chess.Game(
                situation = s,
                turns = sit.turns,
                startedAtTurn = sit.turns,
                clock = c.clock.map(_.config.toClock)
              )
              if (Forsyth.>>(game) == Forsyth.initial) makeChess(chess.variant.Standard) -> none
              else game                                                                  -> baseState
          }
          val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
          val game = Game
            .make(
              chess = chessGame,
              whitePlayer = Player.make(chess.White, c.finalColor.fold(challengerUser, destUser), perfPicker),
              blackPlayer = Player.make(chess.Black, c.finalColor.fold(destUser, challengerUser), perfPicker),
              mode = if (chessGame.board.variant.fromPosition) Mode.Casual else c.mode,
              source = Source.Friend,
              daysPerTurn = c.daysPerTurn,
              pgnImport = None
            )
            .withId(c.id)
            .pipe { g =>
              state.fold(g) {
                case sit @ SituationPlus(Situation(board, _), _) =>
                  g.copy(
                    chess = g.chess.copy(
                      situation = g.situation.copy(
                        board = g.board.copy(
                          history = board.history,
                          variant = chess.variant.FromPosition
                        )
                      ),
                      turns = sit.turns
                    )
                  )
              }
            }
            .start
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
        }
    }

}
