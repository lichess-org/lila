package lila.monitor

import lila.common.PimpedConfig._
import akka.actor._
import com.typesafe.config.Config
import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import play.api.libs.concurrent.Execution.Implicits._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env) {

  private val ActorName = config getString "actor.name"
  private val HubName = config getString "actor.name"
  private val Timeout = config duration "timeout"
  private val WebsocketUidTtl = config duration "socket.uid.ttl"

  lazy val socket = system.actorOf(
    Props(new Hub(timeout = WebsocketUidTtl)), name = HubName)

  // lazy val socket = new Socket(hub = hub)

  lazy val reporting = system.actorOf(
    Props(new Reporting(
      rpsProvider = rpsProvider,
      mpsProvider = mpsProvider,
      db = db,
      hub = hub
    )), name = ActorName)

  // requests per second
  val rpsProvider = new RpsProvider(Timeout)

  // moves per second
  val mpsProvider = new RpsProvider(Timeout)
}

object Env {

  lazy val current = "[boot] monitor" describes new Env(
    config = lila.common.PlayApp loadConfig "monitor",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current)
}
