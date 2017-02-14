package lila.site

import akka.actor._
import com.typesafe.config.Config
import play.api.libs.concurrent.Akka.system

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    hub: lila.hub.Env,
    system: ActorSystem
) {

  private val SocketUidTtl = config duration "socket.uid.ttl"
  private val SocketName = config getString "socket.name"

  private val socket = system.actorOf(
    Props(new Socket(timeout = SocketUidTtl)), name = SocketName
  )

  lazy val socketHandler = new SocketHandler(socket, hub)

  lazy val apiSocketHandler = new ApiSocketHandler(socket, hub)
}

object Env {

  lazy val current = "site" boot new Env(
    config = lila.common.PlayApp loadConfig "site",
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system
  )
}
