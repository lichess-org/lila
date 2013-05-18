package controllers

import lila.app._
import lila.game.Event
import lila.socket.actorApi.Forward
import lila.hub.actorApi.map.Ask
import lila.game.Game.{ takeGameId, takePlayerId }
import makeTimeout.large

import akka.pattern.ask
import play.api.mvc._, Results._

trait RoundEventPerformer {

  protected def performAndRedirect(fullId: String, makeMessage: String ⇒ Any) =
    Action {
      Async {
        perform(fullId, makeMessage) inject Redirect(routes.Round.player(fullId))
      }
    }

  protected def perform(fullId: String, makeMessage: String ⇒ Any): Fu[List[Event]] =
    Env.round.roundMap ?
      Ask(takeGameId(fullId), makeMessage(takePlayerId(fullId))) mapTo
      manifest[List[Event]] logFailure
      "[round] fail to perform on game %s".format(fullId) 

  protected def sendEvents(gameId: String)(events: List[Event]) {
    Env.round.socketHub ! Forward(gameId, events)
  }
}
