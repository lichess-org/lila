package lila.round

import actorApi._
import lila.game.{ Game, GameRepo, PovRef, Pov }
import lila.game.tube.gameTube
import lila.db.api._

import akka.actor.ActorRef

private[round] final class Meddler(
    finisher: Finisher,
    socketHub: ActorRef) {

  def forceAbort(id: String) {
    $find.byId(id) foreach {
      _.fold(logwarn("Cannot abort missing game " + id)) { game ⇒
        (finisher forceAbort game) fold (
          err ⇒ logwarn(err.shows),
          _ foreach { events ⇒ socketHub ! GameEvents(game.id, events) }
        )
      }
    }
  }

  def resign(pov: Pov) {
    (finisher resign pov).fold(
      err ⇒ logwarn(err.shows),
      _ foreach { events ⇒ socketHub ! GameEvents(pov.game.id, events) }
    )
  }

  def resign(povRef: PovRef) {
    GameRepo pov povRef foreach {
      _.fold(logwarn("Cannot resign missing game " + povRef))(resign)
    }
  }

  def finishAbandoned(game: Game) {
    game.abandoned.fold(
      finisher.resign(Pov(game, game.player))
        .prefixFailuresWith("Finish abandoned game " + game.id)
        .fold(err ⇒ logwarn(err.shows), _.void),
      logwarn("Game is not abandoned")
    )
  }
}
