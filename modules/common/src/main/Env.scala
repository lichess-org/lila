package lila.common

import play.api.Configuration
import io.methvin.play.autoconfig._

import config._

final class Env(
    appConfig: Configuration
) {

  val netConfig = appConfig.get[NetConfig]("net")(AutoConfig.loader)
}
