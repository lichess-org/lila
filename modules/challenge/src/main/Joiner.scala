package lila.challenge

import akka.actor.ActorSelection
import akka.pattern.ask

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.{ Situation, Mode }
import lila.game.{ GameRepo, Game, Pov, Source, Player, PerfPicker }
import lila.user.{ User, UserRepo }

private[challenge] final class Joiner(onStart: String => Unit) {

  def apply(c: Challenge, destUser: Option[User]): Fu[Option[Pov]] =
    GameRepo exists c.id flatMap {
      case true => fuccess(None)
      case false =>
        c.challengerUserId.??(UserRepo.byId) flatMap { challengerUser =>

          def makeChess(variant: chess.variant.Variant): chess.Game =
            chess.Game(board = chess.Board init variant, clock = c.clock.map(_.chessClock))

          val baseState = c.initialFen.ifTrue(c.variant == chess.variant.FromPosition) flatMap Forsyth.<<<
          val (chessGame, state) = baseState.fold(makeChess(c.variant) -> none[SituationPlus]) {
            case sit@SituationPlus(Situation(board, color), _) =>
              val game = chess.Game(
                board = board,
                player = color,
                turns = sit.turns,
                startedAtTurn = sit.turns,
                clock = c.clock.map(_.chessClock))
              if (Forsyth.>>(game) == Forsyth.initial) makeChess(chess.variant.Standard) -> none
              else game -> baseState
          }
          val realVariant = chessGame.board.variant
          def makePlayer(color: chess.Color, userOption: Option[User]) = Player.make(color, None) |> { p =>
            userOption.fold(p) { user =>
              p.withUser(user.id, user.perfs(c.perfType))
            }
          }
          val game = Game.make(
            game = chessGame,
            whitePlayer = makePlayer(chess.White, c.finalColor.fold(challengerUser, destUser)),
            blackPlayer = makePlayer(chess.Black, c.finalColor.fold(destUser, challengerUser)),
            mode = (realVariant == chess.variant.FromPosition).fold(Mode.Casual, c.mode),
            variant = realVariant,
            source = (realVariant == chess.variant.FromPosition).fold(Source.Position, Source.Friend),
            daysPerTurn = c.daysPerTurn,
            pgnImport = None).copy(id = c.id).|> { g =>
              state.fold(g) {
                case sit@SituationPlus(Situation(board, _), _) => g.copy(
                  variant = chess.variant.FromPosition,
                  castleLastMoveTime = g.castleLastMoveTime.copy(
                    lastMove = board.history.lastMove.map(_.origDest),
                    castles = board.history.castles
                  ),
                  turns = sit.turns)
              }
            }.start
          (GameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
        }
    }
}
