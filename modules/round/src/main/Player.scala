package lila.round

import chess.{ Status, Role, Color }
import chess.Pos.posAt
import chess.format.Forsyth
import lila.ai.Ai
import lila.game.{ Game, GameRepo, PgnRepo, Pov, Progress }
import actorApi.round.{ HumanPlay, AiPlay, PlayResult }
import lila.hub.actorApi.Tell

private[round] final class Player(
    ai: Ai,
    notifyMove: (String, String, Option[String]) ⇒ Unit,
    finisher: Finisher,
    roundMap: lila.hub.ActorLazyRef) {

  def human(play: HumanPlay)(pov: Pov): Fu[Events] = play match {
    case HumanPlay(playerId, origS, destS, promS, blur, lag, onFailure) ⇒ pov match {
      case Pov(game, color) if (game playableBy color) ⇒
        PgnRepo get game.id flatMap { pgnString ⇒
          (for {
            orig ← posAt(origS) toValid "Wrong orig " + origS
            dest ← posAt(destS) toValid "Wrong dest " + destS
            promotion = Role promotable promS
            chessGame = game.toChess withPgnMoves pgnString
            newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
            (newChessGame, move) = newChessGameAndMove
          } yield game.update(newChessGame, move, blur)).prefixFailuresWith(playerId + " - ").fold(fufail(_), {
            case (progress, pgn) ⇒
              (GameRepo save progress) >>
                PgnRepo.save(pov.gameId, pgn) >>-
                notifyProgress(progress) >>
                progress.game.finished.fold(
                  moveFinish(progress.game, color) map { progress.events ::: _ }, {
                    if (progress.game.playableByAi) roundMap ! Tell(game.id, AiPlay(onFailure))
                    fuccess(progress.events)
                  })
          })
        } addFailureEffect onFailure
    }
  }

  def ai(play: AiPlay)(game: Game): Fu[Events] =
    (game.playable && game.player.isAi).fold(
      (game.variant.exotic ?? { GameRepo initialFen game.id }) zip
        (PgnRepo get game.id) flatMap {
          case (fen, pgn) ⇒
            ai.play(game.toChess, pgn, fen, ~game.aiLevel) flatMap {
              case (newChessGame, move) ⇒ {
                val (progress, pgn2) = game.update(newChessGame, move)
                (GameRepo save progress) >>
                  PgnRepo.save(game.id, pgn2) >>-
                  notifyProgress(progress) >>
                  moveFinish(progress.game, game.turnColor) map { progress.events ::: _ }
              }
            }
        } addFailureEffect play.onFailure,
      fufail("not AI turn")
    )

  private def notifyProgress(progress: Progress) {
    notifyMove(
      progress.game.id,
      Forsyth exportBoard progress.game.toChess.board,
      progress.game.lastMove)
  }

  private def moveFinish(game: Game, color: Color): Fu[Events] = game.status match {
    case Status.Mate                               ⇒ finisher(game, _.Mate, Some(color))
    case status @ (Status.Stalemate | Status.Draw) ⇒ finisher(game, _ ⇒ status)
    case _                                         ⇒ fuccess(Nil)
  }
}
