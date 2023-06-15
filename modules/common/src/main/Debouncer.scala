package lila.common

import akka.actor.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

final class Debouncer[Id](duration: FiniteDuration, initialCapacity: Int = 64)(
    f: Id => Unit
)(using ec: Executor, scheduler: Scheduler):

  // can't use a boolean or int,
  // the ConcurrentHashMap uses weird defaults instead of null for missing values
  private enum Queued:
    case Another, Empty

  private val debounces = ConcurrentHashMap[Id, Queued](initialCapacity)

  def push(id: Id): Unit = debounces
    .compute(
      id,
      (_, prev) =>
        Option(prev) match {
          case None =>
            f(id)
            scheduler.scheduleOnce(duration) { runScheduled(id) }
            Queued.Empty
          case _ => Queued.Another
        }
    )
    .unit

  private def runScheduled(id: Id): Unit = debounces
    .computeIfPresent(
      id,
      (_, queued) =>
        if (queued == Queued.Another) {
          f(id)
          scheduler.scheduleOnce(duration) { runScheduled(id) }
          Queued.Empty
        } else nullToRemove
    )
    .unit

  private[this] var nullToRemove: Queued = _
