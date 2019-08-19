package lidraughts.round

import akka.actor.{ ActorSystem, Cancellable }
import scala.concurrent.duration._

import lidraughts.hub.actorApi.Deploy

private final class DeployPersistence(system: ActorSystem) {

  private var ongoing: Option[Cancellable] = None

  def isEnabled() = ongoing.isDefined

  def enable(): Unit = {
    logger.warn("Enabling round persistence")
    cancel()
    ongoing = system.scheduler.scheduleOnce(7.minutes) {
      logger.warn("Expiring round persistence")
      ongoing = none
    }.some
  }

  def cancel(): Unit = {
    logger.warn("Cancelling round persistence")
    ongoing.foreach(_.cancel())
    ongoing = none
  }

  system.lidraughtsBus.subscribeFun('deploy) {
    case _: Deploy => enable()
  }
}
