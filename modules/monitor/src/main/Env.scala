package lila.monitor

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val ActorName = config getString "actor.name"
  private val SocketName = config getString "socket.name"
  private val RpsIntervall = config duration "rps.interval"
  private val SocketUidTtl = config duration "socket.uid.ttl"

  private lazy val socket = system.actorOf(
    Props(new Socket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket, hub)

  val reporting = system.actorOf(
    Props(new Reporting(
      rpsProvider = rpsProvider,
      mpsProvider = mpsProvider,
      socket = socket,
      db = db,
      hub = hub
    )), name = ActorName)

  {
    import scala.concurrent.duration._

    scheduler.message(1 seconds) {
      reporting -> lila.hub.actorApi.monitor.Update
    }
  }

  // requests per second
  private lazy val rpsProvider = new RpsProvider(RpsIntervall)

  // moves per second
  private lazy val mpsProvider = new RpsProvider(RpsIntervall)
}

object Env {

  lazy val current = "[boot] monitor" describes new Env(
    config = lila.common.PlayApp loadConfig "monitor",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
