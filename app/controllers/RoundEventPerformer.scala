package controllers

import lila.app._
import lila.game.Event
import lila.round.actorApi.GameEvents
import lila.game.Game.takeGameId

import play.api.mvc._, Results._

trait RoundEventPerformer {

  protected type FuValidEvents = Fu[Valid[List[Event]]]

  protected def performAndRedirect(fullId: String, op: String ⇒ FuValidEvents) =
    Action {
      perform(fullId, op)
      Redirect(routes.Round.player(fullId))
    }

  protected def perform(fullId: String, op: String ⇒ FuValidEvents) {
    op(fullId) foreach { validEvents ⇒
      validEvents.fold(
        err ⇒ logwarn("[round] fail to perform on game %s\n%s".format(fullId, err)),
        performEvents(fullId)
      )
    }
  }

  protected def performEvents(fullId: String)(events: List[Event]) {
    Env.round.socketHub ! GameEvents(takeGameId(fullId), events)
  }
}
