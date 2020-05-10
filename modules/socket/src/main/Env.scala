package lidraughts.socket

import akka.actor._
import com.typesafe.config.Config

import actorApi._

final class Env(
    system: ActorSystem,
    settingStore: lidraughts.memo.SettingStore.Builder
) {

  import scala.concurrent.duration._

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }

  val socketDebugSetting = settingStore[Boolean](
    "socketDebug",
    default = false,
    text = "Send extra debugging to websockets.".some
  )
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lidraughts.common.PlayApp.system,
    settingStore = lidraughts.memo.Env.current.settingStore
  )
}
