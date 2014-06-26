package lila.importer

import akka.actor.ActorRef
import chess.Color
import chess.format.UciMove
import lila.game.{ Game, Player, Source, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._
import scala.concurrent.duration._

final class Live(
    roundMap: ActorRef) {

  def create = {
    val variant = chess.Variant.Standard
    val g = Game.make(
      game = chess.Game(variant),
      whitePlayer = Player.white,
      blackPlayer = Player.black,
      mode = chess.Mode.Casual,
      variant = variant,
      source = Source.ImportLive,
      pgnImport = none).start
    GameRepo insertDenormalized g inject g
  }

  def move(id: String, move: String, ip: String) =
    GameRepo game id flatMap {
      _ filter (_.playable) match {
        case None => fufail("No such playing game: " + id)
        case Some(game) => UciMove(move) match {
          case None => move match {
            case "1-0"     => fuccess {
              roundMap ! Tell(game.id, Resign(game.blackPlayer.id))
            }
            case "0-1"     => fuccess {
              roundMap ! Tell(game.id, Resign(game.whitePlayer.id))
            }
            case "1/2-1/2" => fuccess {
              roundMap ! Tell(game.id, DrawForce)
            }
            case m         => fufail("Invalid move: " + m)
          }
          case Some(uci) => fuccess {
            applyMove(Pov(game, game.player.color), uci, ip)
          }
        }
      }
    }

  private def applyMove(pov: Pov, move: UciMove, ip: String) {
    roundMap ! Tell(pov.gameId, HumanPlay(
      playerId = pov.playerId,
      ip = ip,
      orig = move.orig.toString,
      dest = move.dest.toString,
      prom = move.promotion map (_.name),
      blur = false,
      lag = 0.millis,
      onFailure = _ => ()
    ))
  }
}
