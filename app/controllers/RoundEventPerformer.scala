package controllers

import lila.app._
import lila.game.Event
import lila.socket.actorApi.Forward
import lila.hub.actorApi.map.{ Tell, Ask }
import lila.round.actorApi.round.Await
import lila.game.Game.{ takeGameId, takePlayerId }
import makeTimeout.large

import akka.pattern.ask
import play.api.mvc._, Results._

trait RoundEventPerformer {

  protected def performAndRedirect(fullId: String, makeMessage: String ⇒ Any) = Action {
    Async {
      perform(fullId, makeMessage) inject Redirect(routes.Round.player(fullId))
    }
  }

  protected def perform(fullId: String, makeMessage: String ⇒ Any): Funit = {
    Env.round.roundMap ! Tell(
      takeGameId(fullId),
      makeMessage(takePlayerId(fullId))
    )
    Env.round.roundMap ? Ask(takeGameId(fullId), Await) void
  }
}
