package lila.socket

import akka.actor._
import com.typesafe.config.Config
import io.lettuce.core._
import scala.concurrent.duration._

import actorApi._

final class Env(
    system: ActorSystem,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle,
    hub: lila.hub.Env
) {

  private val RedisUri = config getString "redis.uri"

  val population = new SocketPopulation(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  val remoteSocket = new RemoteSocket(
    redisClient = RedisClient create RedisURI.create(RedisUri),
    notificationActor = hub.notification,
    setNb = nb => population ! actorApi.RemoteNbMembers(nb),
    bus = system.lilaBus,
    lifecycle = lifecycle
  )

  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "socket",
    lifecycle = lila.common.PlayApp.lifecycle,
    hub = lila.hub.Env.current
  )
}
