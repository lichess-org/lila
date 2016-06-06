package lila.stripe

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config) {

  val publicKey = config getString "keys.public"

  private lazy val client = new StripeClient(StripeClient.Config(
    endpoint = config getString "endpoint",
    publicKey = publicKey,
    privateKey = config getString "keys.private"))

  lazy val api = new StripeApi(client)
}

object Env {

  lazy val current: Env = "stripe" boot new Env(
    // system = lila.common.PlayApp.system,
    // getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "stripe")
}
