package lila.push

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config) {

    private val AerogearConfig = Aerogear.Config(
      url = config getString "aerogear.url",
      variantId = config getString "aerogear.variant_id",
      secret = config getString "aerogear.secret")

  lazy val aerogear = new Aerogear(AerogearConfig)
}

object Env {

  lazy val current: Env = "push" boot new Env(
    config = lila.common.PlayApp loadConfig "push")
}
