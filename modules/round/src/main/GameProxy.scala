package lila.round

import akka.actor.{ Cancellable, Scheduler }
import scala.concurrent.duration._
import scala.util.Success

import chess.Color
import lila.game.{ Game, GameRepo, Pov, Progress }

// NOT thread safe
final private class GameProxy(
    id: Game.ID,
    dependencies: GameProxy.Dependencies
)(implicit ec: scala.concurrent.ExecutionContext) {

  import GameProxy._
  import dependencies._

  private[round] def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress.dropEvents)(_ withGame progress.game).some
    if (shouldFlushProgress(progress)) flushProgress()
    else fuccess(scheduleFlushProgress())
  }

  def update(f: Game => Game): Funit =
    withGame { g =>
      fuccess(set(f(g)))
    }

  private[round] def saveAndFlush(progress: Progress): Funit = {
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress)(_ withGame progress.game).some
    flushProgress()
  }

  private def set(game: Game): Unit = {
    cache = fuccess(game.some)
  }

  private[round] def setFinishedGame(game: Game): Unit = {
    scheduledFlush.cancel()
    set(game)
    dirtyProgress = none
  }

  // convenience helpers

  def withPov[A](color: Color)(f: Pov => Fu[A]): Fu[A] =
    withGame(g => f(Pov(g, color)))

  def withPov[A](playerId: Game.PlayerId)(f: Option[Pov] => Fu[A]): Fu[A] =
    withGame(g => f(Pov(g, playerId.value)))

  def withGame[A](f: Game => Fu[A]): Fu[A] =
    cache.value match {
      case Some(Success(Some(g))) => f(g)
      case Some(Success(None))    => fufail(s"No proxy game: $id")
      case _ =>
        cache flatMap {
          case None    => fufail(s"No proxy game: $id")
          case Some(g) => f(g)
        }
    }

  def withGameOptionSync[A](f: Game => A): Option[A] =
    cache.value match {
      case Some(Success(Some(g))) => Some(f(g))
      case _                      => None
    }

  def terminate() = flushProgress()

  // internals

  private var dirtyProgress: Option[Progress] = None
  private var scheduledFlush: Cancellable     = emptyCancellable

  private def shouldFlushProgress(p: Progress) =
    p.statusChanged || p.game.isSimul || (
      p.game.hasCorrespondenceClock && !p.game.hasAi && p.game.rated
    )

  private def scheduleFlushProgress(): Unit = {
    scheduledFlush.cancel()
    scheduledFlush = scheduler.scheduleOnce(scheduleDelay) { flushProgress().unit }
  }

  private def flushProgress(): Funit = {
    scheduledFlush.cancel()
    dirtyProgress ?? gameRepo.update addEffect { _ =>
      dirtyProgress = none
    }
  }

  private[this] var cache: Fu[Option[Game]] = fetch

  private[this] def fetch = gameRepo game id
}

private object GameProxy {

  class Dependencies(
      val gameRepo: GameRepo,
      val scheduler: Scheduler
  )

  // must be way under the round asyncActor termination delay (60s)
  private val scheduleDelay = 30.seconds

  private val emptyCancellable = new Cancellable {
    def cancel()    = true
    def isCancelled = true
  }
}
