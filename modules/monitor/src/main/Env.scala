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
  private val SocketName = config getString "socket.name"
  private val RpsIntervall = config duration "rps.interval"
  private val SocketUidTtl = config duration "socket.uid.ttl"

  lazy val socket = system.actorOf(
    Props(new Socket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket)

  lazy val reporting = system.actorOf(
    Props(new Reporting(
      rpsProvider = rpsProvider,
      mpsProvider = mpsProvider,
      db = db,
      hub = hub
    )), name = ActorName)

  // requests per second
  private lazy val rpsProvider = new RpsProvider(RpsIntervall)

  // moves per second
  private lazy val mpsProvider = new RpsProvider(RpsIntervall)
}

object Env {

  lazy val current = "[boot] monitor" describes new Env(
    config = lila.common.PlayApp loadConfig "monitor",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current)
}
