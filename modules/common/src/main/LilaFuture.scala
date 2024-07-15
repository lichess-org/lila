package lila.common

import akka.actor.Scheduler

import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

object LilaFuture:

  def delay[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: Executor, scheduler: Scheduler): Fu[A] =
    if duration == 0.millis then run
    else akka.pattern.after(duration, scheduler)(run)

  def sleep(duration: FiniteDuration)(using ec: Executor, scheduler: Scheduler): Funit =
    val p = Promise[Unit]()
    scheduler.scheduleOnce(duration)(p.success {})
    p.future

  def makeItLast[A](
      duration: FiniteDuration
  )(run: => Fu[A])(using ec: Executor, scheduler: Scheduler): Fu[A] =
    if duration == 0.millis then run
    else run.zip(akka.pattern.after(duration, scheduler)(funit)).dmap(_._1)

  def retry[T](op: () => Fu[T], delay: FiniteDuration, retries: Int, logger: Option[lila.log.Logger])(using
      ec: Executor,
      scheduler: Scheduler
  ): Fu[T] =
    op().recoverWith:
      case e if retries > 0 =>
        logger.foreach { _.info(s"$retries retries - ${e.getMessage}") }
        akka.pattern.after(delay, scheduler)(retry(op, delay, retries - 1, logger))
