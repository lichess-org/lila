package lila.socket

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._
import redis.clients.jedis._

import actorApi._

final class Env(
    system: ActorSystem,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle,
    hub: lila.hub.Env,
    settingStore: lila.memo.SettingStore.Builder
) {

  private val settings = new {
    val RedisHost = config getString "redis.host"
    val RedisPort = config getInt "redis.port"
  }
  import settings._

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  private val remoteSocket = new RemoteSocket(
    redisPool = new JedisPool(new JedisPoolConfig, RedisHost, RedisPort),
    chanIn = "site-in",
    chanOut = "site-out",
    lifecycle = lifecycle,
    notificationActor = hub.notification,
    setNb = nb => population ! actorApi.RemoteNbMembers(nb),
    bus = system.lilaBus
  )

  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }

  val socketDebugSetting = settingStore[Boolean](
    "socketDebug",
    default = false,
    text = "Send extra debugging to websockets.".some
  )
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "socket",
    lifecycle = lila.common.PlayApp.lifecycle,
    hub = lila.hub.Env.current,
    settingStore = lila.memo.Env.current.settingStore
  )
}
