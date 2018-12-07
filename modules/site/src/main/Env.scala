package lidraughts.site

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.concurrent.Akka.system

import lidraughts.socket.actorApi.SendToFlag

final class Env(
    config: Config,
    hub: lidraughts.hub.Env,
    system: ActorSystem
) {

  private val SocketUidTtl = config duration "socket.uid.ttl"

  private val socket = new Socket(system, SocketUidTtl)

  lazy val socketHandler = new SocketHandler(socket, hub)

  system.lidraughtsBus.subscribeFun('sendToFlag, 'deploy) { case m => socket ! m }
}

object Env {

  lazy val current = "site" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "site",
    hub = lidraughts.hub.Env.current,
    system = lidraughts.common.PlayApp.system
  )
}
