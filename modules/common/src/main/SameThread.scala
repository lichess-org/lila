package lila.common

import scala.concurrent.ExecutionContext

/**
 * For small code blocks that don't need to be run on a separate thread.
 */
object SameThread extends ExecutionContext {

  val logger = lila.log.sameThread

  override def execute(runnable: Runnable): Unit = runnable.run
  override def reportFailure(t: Throwable): Unit = logger.error(t.getMessage, t)
}
