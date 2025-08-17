package lila.round

import akka.actor.{ Cancellable, Scheduler }

import scala.util.Success

import lila.game.GameExt.*
import lila.game.{ GameRepo, Progress }

// NOT thread safe
final private class GameProxy(
    id: GameId,
    dependencies: GameProxy.Dependencies,
    private var cache: Fu[Option[Game]]
)(using Executor):

  import GameProxy.*
  import dependencies.*

  private[round] def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit =
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress.dropEvents)(_.withGame(progress.game)).some
    if shouldFlushProgress(progress) then flushProgress()
    else fuccess(scheduleFlushProgress())

  def update(f: Game => Game): Funit =
    withGame: g =>
      fuccess(set(f(g)))

  private[round] def saveAndFlush(progress: Progress): Funit =
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress)(_.withGame(progress.game)).some
    flushProgress()

  private def set(game: Game): Unit =
    cache = fuccess(game.some)

  private[round] def setFinishedGame(game: Game): Unit =
    scheduledFlush.cancel()
    set(game)
    dirtyProgress = none

  // convenience helpers

  def withPov[A](color: Color)(f: Pov => Fu[A]): Fu[A] =
    withGame(g => f(Pov(g, color)))

  def withPov[A](playerId: GamePlayerId)(f: Option[Pov] => Fu[A]): Fu[A] =
    withGame(g => f(g.playerIdPov(playerId)))

  def withGame[A](f: Game => Fu[A]): Fu[A] =
    cache.value match
      case Some(Success(Some(g))) => f(g)
      case Some(Success(None)) => fufail(s"No proxy game: $id")
      case _ =>
        cache.flatMap:
          case None => fufail(s"No proxy game: $id")
          case Some(g) => f(g)

  def withGameOptionSync[A](f: Game => A): Option[A] =
    cache.value match
      case Some(Success(Some(g))) => Some(f(g))
      case _ => None

  def terminate() = flushProgress()

  def flushProgress(): Funit =
    scheduledFlush.cancel()
    dirtyProgress.so: prog =>
      dirtyProgress = none
      gameRepo.update(prog)

  // internals

  private var dirtyProgress: Option[Progress] = None
  private var scheduledFlush: Cancellable = emptyCancellable

  private def shouldFlushProgress(p: Progress) =
    p.statusChanged || p.game.isSimul || (p.game.hasCorrespondenceClock && p.game.rated.yes)

  private def scheduleFlushProgress(): Unit =
    scheduledFlush.cancel()
    scheduledFlush = scheduler.scheduleOnce(scheduleDelay):
      flushProgress()

private object GameProxy:

  class Dependencies(
      val gameRepo: GameRepo,
      val scheduler: Scheduler
  )

  // must be way under the round asyncActor termination delay (60s)
  private val scheduleDelay = 30.seconds

  private val emptyCancellable = new Cancellable:
    def cancel() = true
    def isCancelled = true
