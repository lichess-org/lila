package controllers

import lila._
import core.CoreEnv
import round.Event
import game.{ DbGame, Pov }

import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

trait RoundEventPerformer {

  def env: CoreEnv

  protected type IOValidEvents = IO[Valid[List[Event]]]

  protected def performAndRedirect(fullId: String, op: String ⇒ IOValidEvents) =
    Action {
      perform(fullId, op).unsafePerformIO
      Redirect(routes.Round.player(fullId))
    }

  protected def perform(fullId: String, op: String ⇒ IOValidEvents): IO[Unit] =
    op(fullId) flatMap { validEvents ⇒
      validEvents.fold(putFailures, performEvents(fullId))
    }

  protected def performEvents(fullId: String)(events: List[Event]): IO[Unit] = io {
    env.round.socket.send(DbGame takeGameId fullId, events)
  }
}
