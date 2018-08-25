package lila.challenge

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.{ Situation, Mode }
import lila.game.{ GameRepo, Game, Pov, Source, Player }
import lila.user.{ User, UserRepo }

private[challenge] final class Joiner(onStart: String => Unit) {

  def apply(c: Challenge, destUser: Option[User]): Fu[Option[Pov]] =
    GameRepo exists c.id flatMap {
      case true => fuccess(None)
      case false =>
        c.challengerUserId.??(UserRepo.byId) flatMap { challengerUser =>

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
              else game -> baseState
          }
          val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
          val game = Game.make(
            chess = chessGame,
            whitePlayer = Player.make(chess.White, c.finalColor.fold(challengerUser, destUser), perfPicker),
            blackPlayer = Player.make(chess.Black, c.finalColor.fold(destUser, challengerUser), perfPicker),
            mode = if (chessGame.board.variant.fromPosition) Mode.Casual else c.mode,
            source = if (chessGame.board.variant.fromPosition) Source.Position else Source.Friend,
            daysPerTurn = c.daysPerTurn,
            pgnImport = None
          ).withId(c.id).|> { g =>
              state.fold(g) {
                case sit @ SituationPlus(Situation(board, _), _) => g.copy(
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
            }.start
          (GameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
        }
    }
}
