package lila.challenge

import scala.util.chaining._

import shogi.format.Forsyth
import shogi.format.Forsyth.SituationPlus
import shogi.{ Color, Mode, Situation }
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
          def makeChess(variant: shogi.variant.Variant): shogi.Game =
            shogi.Game(situation = Situation(variant), clock = c.clock.map(_.config.toClock))

          val baseState = c.initialFen.ifTrue(c.variant.fromPosition) flatMap { fen =>
            Forsyth.<<<@(shogi.variant.FromPosition, fen.value)
          }
          val (shogiGame, state) = baseState.fold(makeChess(c.variant) -> none[SituationPlus]) {
            case sit @ SituationPlus(s, _) =>
              val game = shogi.Game(
                situation = s,
                turns = sit.turns,
                startedAtTurn = sit.turns,
                clock = c.clock.map(_.config.toClock)
              )
              if (Forsyth.>>(game) == Forsyth.initial) makeChess(shogi.variant.Standard) -> none
              else game                                                                  -> baseState
          }
          val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
          val game = Game
            .make(
              shogi = shogiGame,
              sentePlayer = Player.make(shogi.Sente, c.finalColor.fold(challengerUser, destUser), perfPicker),
              gotePlayer = Player.make(shogi.Gote, c.finalColor.fold(destUser, challengerUser), perfPicker),
              mode = if (shogiGame.board.variant.fromPosition) Mode.Casual else c.mode,
              source = Source.Friend,
              daysPerTurn = c.daysPerTurn,
              pgnImport = None
            )
            .withId(c.id)
            .pipe { g =>
              state.fold(g) { case sit @ SituationPlus(Situation(board, _), _) =>
                g.copy(
                  shogi = g.shogi.copy(
                    situation = g.situation.copy(
                      board = g.board.copy(
                        history = board.history,
                        variant = shogi.variant.FromPosition
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
