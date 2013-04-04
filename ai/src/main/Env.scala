package lila.ai

import lila.common.PimpedConfig._

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(config: Config) {

  private val settings = new {
  }
  import settings._
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    config = lila.common.PlayApp loadConfig "security")
}
