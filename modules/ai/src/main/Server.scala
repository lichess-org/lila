package lila.ai

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import chess.format.Forsyth

import actorApi._
import lila.analyse.Info
import lila.game.Game

private[ai] final class Server(
    queue: ActorRef,
    config: Config,
    uciMemo: lila.game.UciMemo) {

  def move(uciMoves: List[String], initialFen: Option[String], level: Int): Fu[MoveResult] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    queue ? PlayReq(uciMoves, initialFen map chess960Fen, level) mapTo
      manifest[Option[String]] flatten "[stockfish] play failed" map MoveResult.apply
  }

  def analyse(uciMoves: List[String], initialFen: Option[String], requestedByHuman: Boolean): Fu[List[Info]] = {
    implicit val timeout = makeTimeout {
      if (requestedByHuman) 1.hour else 24.hours
    }
    (queue ? FullAnalReq(uciMoves take config.analyseMaxPlies, initialFen map chess960Fen, requestedByHuman)) mapTo manifest[List[Info]]
  }

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(fen) { situation =>
    fen.replace("KQkq", situation.board.pieces.toList filter {
      case (_, piece) => piece is chess.Rook
    } sortBy {
      case (pos, _) => (pos.y, pos.x)
    } map {
      case (pos, piece) => piece.color.fold(pos.file.toUpperCase, pos.file)
    } mkString "")
  }
}
