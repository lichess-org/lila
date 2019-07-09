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
    hub: lila.hub.Env,
    settingStore: lila.memo.SettingStore.Builder
) {

  private val RedisUri = config getString "redis.uri"

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  private val remoteSocket = new RemoteSocket(
    redisClient = RedisClient create RedisURI.create(RedisUri),
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

  import lila.memo.SettingStore.Regex._
  import lila.memo.SettingStore.Formable.regexFormable
  val socketRemoteUsersSetting = settingStore[scala.util.matching.Regex](
    "socketRemoteUsers",
    default = "".r,
    text = "Regex selecting user IDs using remote socket".some
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
