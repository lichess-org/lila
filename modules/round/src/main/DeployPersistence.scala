package lila.round

import akka.actor.{ Cancellable, Scheduler }
import scala.concurrent.duration._

import lila.hub.actorApi.Deploy

final private class DeployPersistence(scheduler: Scheduler)(implicit ec: scala.concurrent.ExecutionContext) {

  private var ongoing: Option[Cancellable] = None

  def isEnabled() = ongoing.isDefined

  def enable(): Unit = {
    cancel()
    logger.warn("Enabling round persistence")
    ongoing = scheduler
      .scheduleOnce(7.minutes) {
        logger.warn("Expiring round persistence")
        ongoing = none
      }
      .some
  }

  def cancel(): Unit =
    ongoing foreach { o =>
      logger.warn("Cancelling round persistence")
      o.cancel()
      ongoing = none
    }

  lila.common.Bus.subscribeFun("deploy") {
    case _: Deploy => enable()
  }
}
