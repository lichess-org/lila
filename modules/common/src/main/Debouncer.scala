package lila.common

import java.util.concurrent.ConcurrentHashMap

final class Debouncer[Id](duration: FiniteDuration, initialCapacity: Int = 64)(
    f: Id => Unit
)(using Executor)(using scheduler: Scheduler):

  // can't use a boolean or int,
  // the ConcurrentHashMap uses weird defaults instead of null for missing values
  private enum Queued:
    case Another, Empty

  private val debounces = ConcurrentHashMap[Id, Queued](initialCapacity)

  def push(id: Id): Unit = debounces
    .compute(
      id,
      (_, prev) =>
        Option(prev) match
          case None =>
            f(id)
            scheduler.scheduleOnce(duration) { runScheduled(id) }
            Queued.Empty
          case _ => Queued.Another
    )

  private def runScheduled(id: Id): Unit = debounces
    .computeIfPresent(
      id,
      (_, queued) =>
        if queued == Queued.Another then
          f(id)
          scheduler.scheduleOnce(duration) { runScheduled(id) }
          Queued.Empty
        else nullToRemove
    )

  private var nullToRemove: Queued = scala.compiletime.uninitialized
