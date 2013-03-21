package lila.websocket

import com.typesafe.config.Config
import lila.common.ConfigPimps._

final class Env(config: Config) {

  val MetahubTimeout = config duration "metahub.timeout"
  val MetahubName = config getString "metahub.name"
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "websocket")
}
