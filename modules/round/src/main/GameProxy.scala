package lila.round

import akka.actor.{ Cancellable, Scheduler }
import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, GameDiff, Progress, Pov, GameRepo }
import ornicar.scalalib.Zero

private final class GameProxy(
    id: Game.ID,
    alwaysPersist: () => Boolean,
    persistIfSpeedIdHigherThan: () => Int,
    scheduler: Scheduler
) {

  import GameProxy._

  def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    dirtyProgress = dirtyProgress.fold(progress.dropEvents)(_ withGame progress.game).some
    if (shouldFlushProgress(progress)) flushProgress
    else fuccess(scheduleFlushProgress)
  }

  // update both the cache and the DB
  def persistAndSet(p: GameRepo.type => Funit, s: Game => Game): Funit =
    game.map {
      _ ?? { g => set(s(g)) }
    } >> p(GameRepo)

  def persist(f: GameRepo.type => Funit): Funit = f(GameRepo)

  private def set(game: Game): Unit = {
    cache = fuccess(game.some)
  }

  private[round] def setFinishedGame(game: Game): Unit = {
    scheduledFlush.cancel()
    set(game)
    dirtyProgress = none
  }

  // convenience helpers

  def pov(color: Color) = game.dmap {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.dmap {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  // internals

  private var dirtyProgress: Option[Progress] = None
  private var scheduledFlush: Cancellable = emptyCancellable

  private def shouldFlushProgress(p: Progress) =
    alwaysPersist() || p.game.isSimul || p.game.speed.id > persistIfSpeedIdHigherThan() || p.statusChanged

  private def scheduleFlushProgress = {
    scheduledFlush.cancel()
    scheduledFlush = scheduler.scheduleOnce(scheduleDelay)(flushProgress)
  }

  private def flushProgress = {
    scheduledFlush.cancel()
    dirtyProgress ?? GameRepo.update addEffect { _ =>
      dirtyProgress = none
    }
  }

  private[this] var cache: Fu[Option[Game]] = fetch

  private[this] def fetch = GameRepo game id
}

object GameProxy {

  type Save = Progress => Funit

  private val scheduleDelay = 15.seconds

  private val emptyCancellable = new Cancellable {
    def cancel() = true
    def isCancelled = true
  }
}
