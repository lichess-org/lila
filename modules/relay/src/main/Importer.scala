package lila.relay

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.pattern.after
import chess.format.UciMove
import chess.{ Color, Move, PromotableRole, Pos }
import lila.game.{ Game, Player, Source, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    ip: String,
    scheduler: akka.actor.Scheduler) {

  def create(data: Parser.Data) = chess.format.pgn.Reader.full(data.pgn).future flatMap { replay =>
    val g = Game.make(
      game = replay.setup,
      whitePlayer = Player.white withName data.white,
      blackPlayer = Player.black withName data.black,
      mode = chess.Mode.Casual,
      variant = replay.setup.board.variant,
      source = Source.Relay,
      pgnImport = none).start

    def applyMoves(pov: Pov, moves: List[Move]): Funit = moves match {
      case Nil => after(delay, scheduler)(funit)
      case m :: rest =>
        after(delay, scheduler)(Future(applyMove(pov, m, ip))) >>
          applyMoves(!pov, rest)
    }

    (GameRepo insertDenormalized g) >>
      applyMoves(Pov(g, Color.white), replay.chronoMoves) inject g
  }

  def move(id: String, move: String) = GameRepo game id flatMap {
    _ filter (g => g.playable && g.isFicsRelay) match {
      case None => fufail("No such playing game: " + id)
      case Some(game) => chess.format.pgn.Parser.MoveParser(move).flatMap {
        _(game.toChess.situation)
      }.toOption match {
        case None => move match {
          case "1-0" => fuccess {
            roundMap ! Tell(game.id, Resign(game.blackPlayer.id))
          }
          case "0-1" => fuccess {
            roundMap ! Tell(game.id, Resign(game.whitePlayer.id))
          }
          case "1/2-1/2" => fuccess {
            roundMap ! Tell(game.id, DrawForce)
          }
          case m => fufail("Invalid move: " + m)
        }
        case Some(move) => fuccess {
          applyMove(Pov(game, game.player.color), move, ip)
        }
      }
    }
  }

  private def applyMove(pov: Pov, move: Move, ip: String) {
    roundMap ! Tell(pov.gameId, HumanPlay(
      playerId = pov.playerId,
      ip = ip,
      orig = move.orig.toString,
      dest = move.dest.toString,
      prom = move.promotion map (_.name),
      blur = false,
      lag = 0.millis,
      onFailure = println
    ))
  }
}
