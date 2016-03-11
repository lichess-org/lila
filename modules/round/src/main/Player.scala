package lila.round

import chess.format.{ Forsyth, Uci }
import chess.Pos.posAt
import chess.{ Status, Role, Color, MoveOrDrop }
import scalaz.Validation.FlatMap._

import actorApi.round.{ HumanPlay, AiPlay, DrawNo, TakebackNo, PlayResult, Cheat, ForecastPlay }
import akka.actor.ActorRef
import lila.game.{ Game, GameRepo, Pov, Progress, UciMemo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.MoveEvent

private[round] final class Player(
    engine: lila.ai.Client,
    bus: lila.common.Bus,
    finisher: Finisher,
    cheatDetector: CheatDetector,
    uciMemo: UciMemo) {

  def human(play: HumanPlay, round: ActorRef)(pov: Pov): Fu[Events] = play match {
    case HumanPlay(playerId, uci, blur, lag, promiseOption) => pov match {
      case Pov(game, color) if game playableBy color => {
        (uci match {
          case Uci.Move(orig, dest, prom) => game.toChess.apply(orig, dest, prom, lag) map {
            case (ncg, move) => ncg -> (Left(move): MoveOrDrop)
          }
          case Uci.Drop(role, pos) => game.toChess.drop(role, pos, lag) map {
            case (ncg, drop) => ncg -> (Right(drop): MoveOrDrop)
          }
        }).map {
          case (newChessGame, moveOrDrop) =>
            game.update(newChessGame, moveOrDrop, blur, lag.some) -> moveOrDrop
        }.prefixFailuresWith(s"$pov ")
          .fold(errs => ClientErrorException.future(errs.shows), fuccess).flatMap {
            case (progress, moveOrDrop) =>
              (GameRepo save progress).mon(_.round.move.save) >>-
                (pov.game.hasAi ! uciMemo.add(pov.game, moveOrDrop)) >>-
                notifyMove(moveOrDrop, progress.game) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, color) map { progress.events ::: _ }, {
                    cheatDetector(progress.game) addEffect {
                      case Some(color) => round ! Cheat(color)
                      case None =>
                        if (progress.game.playableByAi) round ! AiPlay
                        if (pov.opponent.isOfferingDraw) round ! DrawNo(pov.player.id)
                        if (pov.player.isProposingTakeback) round ! TakebackNo(pov.player.id)
                        moveOrDrop.left.toOption.ifTrue(pov.game.forecastable).foreach { move =>
                          round ! ForecastPlay(move)
                        }
                    } inject progress.events
                  }) >>- promiseOption.foreach(_.success(()))
          }
      } addFailureEffect { e =>
        promiseOption.foreach(_ failure e)
      }
      case Pov(game, _) if game.finished           => ClientErrorException.future(s"$pov game is finished")
      case Pov(game, _) if game.aborted            => ClientErrorException.future(s"$pov game is aborted")
      case Pov(game, color) if !game.turnOf(color) => ClientErrorException.future(s"$pov not your turn")
      case _                                       => ClientErrorException.future(s"$pov move refused for some reason")
    }
  }

  def ai(game: Game): Fu[Progress] =
    (game.playable && game.player.isAi).fold(
      engine.play(game, game.aiLevel | 1) flatMap {
        case lila.ai.actorApi.PlayResult(progress, move, _) =>
          notifyMove(Left(move), progress.game)
          moveFinish(progress.game, game.turnColor) map { progress.++ }
      },
      fufail(s"Not AI turn")
    ) prefixFailure s"[ai play] game ${game.id} turn ${game.turns}"

  private def notifyMove(moveOrDrop: MoveOrDrop, game: Game) {
    val color = moveOrDrop.fold(_.color, _.color)
    bus.publish(MoveEvent(
      gameId = game.id,
      color = color,
      fen = Forsyth exportBoard game.toChess.board,
      move = moveOrDrop.fold(_.toUci.keys, _.toUci.uci),
      mobilePushable = game.mobilePushable,
      opponentUserId = game.player(!color).userId,
      simulId = game.simulId
    ), 'moveEvent)
  }

  private def moveFinish(game: Game, color: Color): Fu[Events] = {
    lazy val winner = game.toChess.situation.winner
    game.status match {
      case Status.Mate                             => finisher.other(game, _.Mate, winner)
      case Status.VariantEnd                       => finisher.other(game, _.VariantEnd, winner)
      case status@(Status.Stalemate | Status.Draw) => finisher.other(game, _ => status)
      case _                                       => fuccess(Nil)
    }
  }
}
