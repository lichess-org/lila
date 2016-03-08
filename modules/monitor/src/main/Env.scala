package lila.monitor

import akka.actor._
import com.typesafe.config.Config
import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.common.PimpedConfig._
import lila.common.WindowCount
import lila.socket.Channel

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val ActorName = config getString "actor.name"
  private val SocketName = config getString "socket.name"
  private val SocketUidTtl = config duration "socket.uid.ttl"

  private lazy val socket = system.actorOf(
    Props(new Socket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket, hub)

  val reporting = system.actorOf(
    Props(new Reporting(
      reqWindowCount = reqWindowCount,
      moveWindowCount = moveWindowCount,
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
  private lazy val reqWindowCount = new WindowCount(1 second)

  // moves per second
  private lazy val moveWindowCount = new WindowCount(1 second)
}

object Env {

  lazy val current = "monitor" boot new Env(
    config = lila.common.PlayApp loadConfig "monitor",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
