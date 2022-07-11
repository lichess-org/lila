package lila.api

import akka.actor.{ ActorSystem, CoordinatedShutdown, Scheduler }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.common.Lilakka

final private class Monitoring(scheduler: Scheduler, shutdown: CoordinatedShutdown)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) {

  scheduler.scheduleOnce(20 seconds) {
    val ecMonitor = new ExecutionContextMonitor(shutdown, 2 seconds)
    ecMonitor.monitor(system.dispatcher, "akka.default-dispatcher")
  }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size).unit
  }
}

// Derived from https://gist.github.com/atamborrino/ff67876df9b862758beade693a77ea97
// Thanks Alex
final private class ExecutionContextMonitor(shutdown: CoordinatedShutdown, interval: FiniteDuration) {

  import java.lang.reflect.Method
  import java.util.concurrent._
  import akka.dispatch.{ Dispatcher, ExecutorServiceDelegate }

  private val logger = lila log "ec"

  private val scheduler = Executors.newSingleThreadScheduledExecutor()

  // Reflection to access Scala protected method
  private val ecGetterForAkkaDispatcher = classOf[Dispatcher].getDeclaredMethod("executorService")
  ecGetterForAkkaDispatcher.setAccessible(true)

  def monitor(ec: Executor, ecName: String): Unit = {

    val mon = new lila.mon.executor(ecName)

    ec match {

      case forkJoinPool: ForkJoinPool =>
        logger.info(s"Monitoring ec $ecName of type ForkJoinPool")
        scheduleMonitoring { () =>
          monitorForkJoinPool(forkJoinPool)
        }

      case dispatcher: Dispatcher =>
        logger.info(s"Monitoring ec $ecName of type akka.dispatch.Dispatcher")
        val ec = ecGetterForAkkaDispatcher.invoke(dispatcher).asInstanceOf[ExecutorServiceDelegate].executor
        ec match {
          case p: ForkJoinPool => scheduleMonitoring { () => monitorForkJoinPool(p) }
          case _ => logger.warn(s"Can not monitor ${ec.getClass} from akka dispatcher $dispatcher")
        }

      case _ => logger.warn(s"Can not register metrics monitoring for execution context $ec")
    }

    def scheduleMonitoring(f: () => Unit) =
      scheduler
        .scheduleAtFixedRate(
          new Runnable { def run() = f() },
          interval.toMillis,
          interval.toMillis,
          TimeUnit.MILLISECONDS
        )
        .unit

    def monitorForkJoinPool(p: ForkJoinPool): Unit = {
      mon.queuedSubmissions.record(p.getQueuedSubmissionCount)
      mon.queuedTasks.record(p.getQueuedSubmissionCount)
      mon.poolSize.record(p.getPoolSize)
      mon.activeThreads.record(p.getActiveThreadCount)
      mon.runningThreads.record(p.getRunningThreadCount)
      mon.steals.update(p.getStealCount.toDouble)
      ()
    }
  }

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stop round socket") { () =>
    fuccess(scheduler.shutdown())
  }
}
