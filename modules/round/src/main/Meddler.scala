package lila.round

import actorApi._, round._
import lila.game.{ Game, GameRepo, PovRef, Pov }
import lila.game.tube.gameTube
import lila.hub.actorApi.Tell
import lila.db.api._

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }

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
