package lila.round

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }

import actorApi._, round._
import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, PovRef, Pov }
import lila.hub.actorApi.map.Tell

private[round] final class Meddler(roundMap: ActorRef, socketHub: ActorRef) {

  def forceAbort(gameId: String) {
    roundMap ! Tell(gameId, AbortForce)
  }

  def resign(pov: Pov) {
    roundMap ! Tell(pov.gameId, Resign(pov.playerId))
  }

  def finishAbandoned(game: Game) {
    game.abandoned ?? {
      roundMap ! Tell(game.id, Resign(game.player.id))
    }
  }
}
