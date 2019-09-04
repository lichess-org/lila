package lila.site

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.concurrent.Akka.system

import lila.socket.actorApi.SendToFlag

final class Env(
    config: Config,
    remoteSocketApi: lila.socket.RemoteSocket,
    population: lila.socket.SocketPopulation,
    hub: lila.hub.Env,
    system: ActorSystem
) {

  private val SocketSriTtl = config duration "socket.sri.ttl"

  private val socket = new Socket(system, SocketSriTtl)

  val remoteSocket = new SiteRemoteSocket(
    remoteSocketApi = remoteSocketApi
  )

  lazy val socketHandler = new SocketHandler(socket, hub)
}

object Env {

  lazy val current = "site" boot new Env(
    config = lila.common.PlayApp loadConfig "site",
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    population = lila.socket.Env.current.population,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system
  )
}
