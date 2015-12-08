package lila.push

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config) {

  private val settings = new {
    val GoogleApiKey = config getString "google.api_key"
  }
  import settings._

  lazy val deviceApi = new DeviceApi(
    googleApiKey = GoogleApiKey)
}

object Env {

  lazy val current: Env = "push" boot new Env(
    config = lila.common.PlayApp loadConfig "push")
}
