package lila.relay

import akka.actor._
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.duration._

private final class RelaySync(
    api: RelayApi
) extends Actor {

  override def preStart {
    logger.info("Start RelaySync")
    context setReceiveTimeout 15.seconds
    scheduleNext
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(1 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      val startAt = nowMillis
      api.currents.map { relay =>
        WS.url(relay.url).get().flatMap { res =>
          api.sync(relay, res.body)
        } recover {
          case e: Exception =>
            logger.warn(s"Fetch $relay", e)
            ()
        }
      }.sequenceFu.chronometer
        .logIfSlow(3000, logger)(_ => "RelaySync.tick")
        .result addEffectAnyway scheduleNext
  }
}
