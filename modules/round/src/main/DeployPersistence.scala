package lila.round

import akka.actor.{ ActorSystem, Cancellable }
import scala.concurrent.duration._

import lila.hub.actorApi.Deploy

private final class DeployPersistence(system: ActorSystem) {

  private var ongoing: Option[Cancellable] = None

  def isEnabled() = ongoing.isDefined

  def enable(): Unit = {
    cancel()
    logger.warn("Enabling round persistence")
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

  system.lilaBus.subscribeFun('deploy) {
    case _: Deploy => enable()
  }
}
