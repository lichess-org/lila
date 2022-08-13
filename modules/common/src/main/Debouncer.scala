package lila.common

import akka.actor._
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

final class Debouncer[Id](duration: FiniteDuration, initialCapacity: Int = 64)(
    f: Id => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {
  import Debouncer._

  private val debounces = new ConcurrentHashMap[Id, Queued](initialCapacity)

  def push(id: Id): Unit = debounces
    .compute(
      id,
      (_, prev) =>
        Option(prev) match {
          case None =>
            f(id)
            scheduler.scheduleOnce(duration) { runScheduled(id) }
            Empty
          case _ => Another
        }
    )
    .unit

  private def runScheduled(id: Id): Unit = debounces
    .computeIfPresent(
      id,
      (_, queued) =>
        if (queued == Another) {
          f(id)
          scheduler.scheduleOnce(duration) { runScheduled(id) }
          Empty
        } else nullToRemove
    )
    .unit

  private[this] var nullToRemove: Queued = _
}

private object Debouncer {

  // can't use a boolean or int,
  // the ConcurrentHashMap uses weird defaults instead of null for missing values
  sealed private trait Queued
  private object Another extends Queued
  private object Empty   extends Queued
}
