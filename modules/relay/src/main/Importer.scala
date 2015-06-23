package lila.relay

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.pattern.after
import chess.format.UciMove
import chess.variant.Standard
import chess.{ Color, Move, PromotableRole, Pos }
import lila.game.{ Game, Player, Source, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._

final class Importer(
    roundMap: ActorRef,
    delay: FiniteDuration,
    ip: String,
    scheduler: akka.actor.Scheduler) {

  def full(gameId: String, data: command.Moves.Game): Fu[Game] =
    chess.format.pgn.Reader.full(data.pgn).future flatMap { replay =>
      GameRepo game gameId flatMap {
        case Some(game) => fuccess(game)
        case None =>
          val game = Game.make(
            game = replay.setup,
            whitePlayer = Player.white withName data.white.name,
            blackPlayer = Player.black withName data.black.name,
            mode = chess.Mode.Casual,
            variant = replay.setup.board.variant,
            source = Source.Relay,
            pgnImport = none).withId(gameId).start
          (GameRepo insertDenormalized game) inject game
      } flatMap { game =>

        def applyMoves(pov: Pov, moves: List[Move]): Funit = moves match {
          case Nil => after(delay, scheduler)(funit)
          case m :: rest =>
            after(delay, scheduler)(Future(applyMove(pov, m, ip))) >>
              applyMoves(!pov, rest)
        }

        val lateMoves = replay.chronoMoves drop game.turns
        println(s"http://en.l.org/$gameId recover ${lateMoves.size} moves ${lateMoves.mkString(" ")}\nfrom ${data}")
        applyMoves(Pov player game, lateMoves) inject game
      }
    }

  def move(id: String, san: String, ply: Int) = GameRepo game id flatMap {
    case Some(game) if !game.playable        => fufail(s"{$id} not playable")
    case Some(game) if !game.isFicsRelay     => fufail(s"{$id} not FICS relay")
    case Some(game) if game.turns != ply - 1 => fufail(s"{$id} can't play ply $ply from ${game.turns}")
    case None                                => fufail(s"{$id} not found")
    case Some(game) => chess.format.pgn.Parser.MoveParser(san, Standard).flatMap {
      _(game.toChess.situation)
    }.toOption match {
      case None => san match {
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

  private def applyMove(pov: Pov, move: Move, ip: String) {
    roundMap ! Tell(pov.gameId, RelayPlay(
      playerId = pov.playerId,
      orig = move.orig.toString,
      dest = move.dest.toString,
      prom = move.promotion map (_.name),
      onFailure = println
    ))
  }
}
