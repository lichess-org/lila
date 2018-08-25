package lidraughts.challenge

import draughts.format.Forsyth
import draughts.format.Forsyth.SituationPlus
import draughts.{ Situation, Mode }
import lidraughts.game.{ GameRepo, Game, Pov, Source, Player }
import lidraughts.user.{ User, UserRepo }

private[challenge] final class Joiner(onStart: String => Unit) {

  def apply(c: Challenge, destUser: Option[User]): Fu[Option[Pov]] =
    GameRepo exists c.id flatMap {
      case true => fuccess(None)
      case false =>
        c.challengerUserId.??(UserRepo.byId) flatMap { challengerUser =>

          def makeDraughts(variant: draughts.variant.Variant): draughts.DraughtsGame =
            draughts.DraughtsGame(situation = Situation(variant), clock = c.clock.map(_.config.toClock))

          val baseState = c.initialFen.ifTrue(c.variant.fromPosition) flatMap { fen =>
            Forsyth.<<<@(draughts.variant.FromPosition, fen.value)
          }
          val (draughtsGame, state) = baseState.fold(makeDraughts(c.variant) -> none[SituationPlus]) {
            case sit @ SituationPlus(s, _) =>
              val game = draughts.DraughtsGame(
                situation = s,
                turns = sit.turns,
                startedAtTurn = sit.turns,
                clock = c.clock.map(_.config.toClock)
              )
              if (Forsyth.>>(game) == Forsyth.initial) makeDraughts(draughts.variant.Standard) -> none
              else game -> baseState
          }
          val perfPicker = (perfs: lidraughts.user.Perfs) => perfs(c.perfType)
          val game = Game.make(
            draughts = draughtsGame,
            whitePlayer = Player.make(draughts.White, c.finalColor.fold(challengerUser, destUser), perfPicker),
            blackPlayer = Player.make(draughts.Black, c.finalColor.fold(destUser, challengerUser), perfPicker),
            mode = if (draughtsGame.board.variant.fromPosition) Mode.Casual else c.mode,
            source = if (draughtsGame.board.variant.fromPosition) Source.Position else Source.Friend,
            daysPerTurn = c.daysPerTurn,
            pdnImport = None
          ).withId(c.id).|> { g =>
              state.fold(g) {
                case sit @ SituationPlus(Situation(board, _), _) => g.copy(
                  draughts = g.draughts.copy(
                    situation = g.situation.copy(
                      board = g.board.copy(
                        history = board.history,
                        variant = draughts.variant.FromPosition
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
