package lila.challenge

import scala.util.chaining._

import shogi.format.forsyth.Sfen
import shogi.format.forsyth.Sfen.SituationPlus
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
          def makeShogi(variant: shogi.variant.Variant): shogi.Game =
            shogi.Game(situation = Situation(variant), clock = c.clock.map(_.config.toClock))

          val baseState = c.initialSfen.ifTrue(c.variant.fromPosition) flatMap {
            _.toSituationPlus(shogi.variant.FromPosition)
          }
          val (shogiGame, state) = baseState.fold(makeShogi(c.variant) -> none[SituationPlus]) {
            case sit @ SituationPlus(s, _) =>
              val game = shogi.Game(
                situation = s,
                plies = sit.plies,
                startedAtPly = sit.plies,
                startedAtMove = sit.moveNumber,
                clock = c.clock.map(_.config.toClock)
              )
              if (c.variant.fromPosition && game.toSfen.initialOf(shogi.variant.Standard)) 
                makeShogi(shogi.variant.Standard) -> none
              else game -> baseState
          }
          val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
          val game = Game
            .make(
              shogi = shogiGame,
              sentePlayer = Player.make(shogi.Sente, c.finalColor.fold(challengerUser, destUser), perfPicker),
              gotePlayer = Player.make(shogi.Gote, c.finalColor.fold(destUser, challengerUser), perfPicker),
              mode = if (shogiGame.variant.fromPosition) Mode.Casual else c.mode,
              source = Source.Friend,
              daysPerTurn = c.daysPerTurn,
              notationImport = None
            )
            .withId(c.id)
            .pipe { g =>
              state.fold(g) { case parsed @ SituationPlus(sit, _) =>
                g.copy(
                  shogi = g.shogi.copy(
                    situation = g.situation.copy(
                      board = sit.board,
                      hands = sit.hands,
                      history = sit.history,
                      variant = shogi.variant.FromPosition
                    ),
                    plies = parsed.plies
                  )
                )
              }
            }
            .start
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
        }
    }

}
