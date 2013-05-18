package controllers

import lila.app._
import lila.game.Event
import lila.socket.actorApi.Forward
import lila.hub.actorApi.map.Tell
import lila.game.Game.{ takeGameId, takePlayerId }
import makeTimeout.large

import play.api.mvc._, Results._

trait RoundEventPerformer {

  protected def performAndRedirect(fullId: String, makeMessage: String ⇒ Any) = Action {
    perform(fullId, makeMessage)
    Redirect(routes.Round.player(fullId))
  }

  protected def perform(fullId: String, makeMessage: String ⇒ Any) {
    Env.round.roundMap ! Tell(
      takeGameId(fullId),
      makeMessage(takePlayerId(fullId))
    )
  }

  protected def sendEvents(gameId: String)(events: List[Event]) {
    Env.round.roundMap ! Tell(gameId, events)
  }
}
