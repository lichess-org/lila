package lila.socket

import akka.actor._
import com.typesafe.config.Config
import io.lettuce.core._
import scala.concurrent.duration._

final class Env(
    system: ActorSystem,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle,
    hub: lila.hub.Env
) {

  private val RedisUri = config getString "redis.uri"

  val remoteSocket = new RemoteSocket(
    redisClient = RedisClient create RedisURI.create(RedisUri),
    notificationActor = hub.notification,
    bus = system.lilaBus,
    lifecycle = lifecycle
  )
  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineUserIds: () => Set[String] = () => remoteSocket.onlineUserIds.get
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "socket",
    lifecycle = lila.common.PlayApp.lifecycle,
    hub = lila.hub.Env.current
  )
}
