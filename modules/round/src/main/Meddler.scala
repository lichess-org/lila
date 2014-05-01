package lila.round

import akka.actor.ActorRef

import actorApi.round._
import lila.game.Pov
import lila.hub.actorApi.map.Tell

private[round] final class Meddler(roundMap: ActorRef, socketHub: ActorRef) {

  def resign(pov: Pov) {
    roundMap ! Tell(pov.gameId, Resign(pov.playerId))
  }
}
