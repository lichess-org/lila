package lila.round

import chess.format.Forsyth
import chess.Pos.posAt
import chess.{ Status, Role, Color }
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
    case HumanPlay(playerId, ip, origS, destS, promS, blur, lag, onFailure) => pov match {
      case Pov(game, color) if (game playableBy color) => {
        (for {
          orig ← posAt(origS) toValid "Wrong orig " + origS
          dest ← posAt(destS) toValid "Wrong dest " + destS
          promotion = Role promotable promS
          newChessGameAndMove ← game.toChess(orig, dest, promotion, lag)
          (newChessGame, move) = newChessGameAndMove
        } yield game.update(newChessGame, move, blur) -> move).prefixFailuresWith(s"$pov ")
          .fold(errs => ClientErrorException.future(errs.shows), fuccess).flatMap {
            case (progress, move) =>
              (GameRepo save progress) >>-
                (pov.game.hasAi ! uciMemo.add(pov.game, move)) >>-
                notifyMove(move, progress.game, ip) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, color) map { progress.events ::: _ }, {
                    cheatDetector(progress.game) addEffect {
                      case Some(color) => round ! Cheat(color)
                      case None =>
                        if (progress.game.playableByAi) round ! AiPlay
                        if (pov.opponent.isOfferingDraw) round ! DrawNo(pov.player.id)
                        if (pov.player.isProposingTakeback) round ! TakebackNo(pov.player.id)
                        if (pov.game.forecastable) round ! ForecastPlay(move)
                    } inject progress.events
                  })
          }
      } addFailureEffect onFailure
      case Pov(game, _) if game.finished           => ClientErrorException.future(s"$pov game is finished")
      case Pov(game, _) if game.aborted            => ClientErrorException.future(s"$pov game is aborted")
      case Pov(game, color) if !game.turnOf(color) => ClientErrorException.future(s"$pov not your turn")
      case _                                       => ClientErrorException.future(s"$pov move refused for some reason")
    }
  }

  def ai(game: Game): Fu[Progress] =
    (game.playable && game.player.isAi).fold(
      engine.play(game, game.aiLevel | 1) flatMap {
        case lila.ai.actorApi.PlayResult(progress, move) =>
          moveFinish(progress.game, game.turnColor) map { progress.++ }
      },
      fufail(s"Not AI turn")
    ) prefixFailure s"[ai play] game ${game.id} turn ${game.turns}"

  private def notifyMove(move: chess.Move, game: Game, ip: String) {
    bus.publish(MoveEvent(
      ip = ip,
      gameId = game.id,
      color = move.color,
      fen = Forsyth exportBoard game.toChess.board,
      move = move.keyString,
      piece = move.piece.forsyth,
      opponentUserId = game.player(!move.color).userId,
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
