package lila.importer

import akka.actor.ActorRef
import chess.format.Uci
import lila.game.{ Game, Player, Source, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._
import scala.concurrent.duration._

final class Live(
    roundMap: ActorRef) {

  def create = {
    val variant = chess.variant.Standard
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

  def move(id: String, move: String) =
    GameRepo game id flatMap {
      _ filter (g => g.playable && g.imported) match {
        case None => fufail("No such playing game: " + id)
        case Some(game) => Uci(move) match {
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
            case m => fufail("Importer invalid move: " + m)
          }
          case Some(uci) => fuccess {
            applyMove(Pov(game, game.player.color), uci)
          }
        }
      }
    }

  private def applyMove(pov: Pov, uci: Uci) {
    roundMap ! Tell(pov.gameId, HumanPlay(
      playerId = pov.playerId,
      uci = uci,
      blur = false,
      lag = 0.millis
    ))
  }
}
